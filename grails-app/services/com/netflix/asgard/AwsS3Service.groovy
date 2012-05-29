/*
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.asgard

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.S3Object
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.InitializingBean

class AwsS3Service implements InitializingBean {

    static transactional = false

    MultiRegionAwsClient<AmazonS3> awsClient
    def grailsApplication
    def awsClientService
    def emailerService
    def taskService

    void afterPropertiesSet() {
        awsClient = new MultiRegionAwsClient<AmazonS3>( { Region region ->
            AmazonS3 client = awsClientService.create(AmazonS3)
            // Unconventional S3 endpoints. http://docs.amazonwebservices.com/general/latest/gr/index.html?rande.html
            if (region != Region.US_EAST_1) { client.setEndpoint("s3-${region}.amazonaws.com") }
            client
        })
    }

    public static final String MANIFEST_SUFFIX = '.manifest.xml'

    void deleteBundle(UserContext userContext, String location) {
        // The bucket name is before the first slash. The key is everything after the first slash.
        String bucket = StringUtils.substringBefore(location, '/')
        String key = StringUtils.substringAfter(location, '/')
        String manifestKey = key + (key.endsWith(MANIFEST_SUFFIX) ? '' : MANIFEST_SUFFIX)
        String manifestContent = getObjectContent(userContext, bucket, manifestKey)

        if (manifestContent) {
            Collection<String> partNames = parsePartsFromManifestContent(manifestContent)
            log.debug "Manifest ${location} references parts: ${partNames}"
            deleteObjectsInBucket(userContext, bucket, partNames)
            if (areAllObjectsDeleted(userContext, bucket, partNames)) {
                log.info "Deleting ${bucket} ${manifestKey}"
                awsClient.by(userContext.region).deleteObject(new DeleteObjectRequest(bucket, manifestKey))
            } else {
                handleError("Failed to delete some parts of S3 bundle ${bucket}/${manifestKey}")
            }
        }
    }

    private Collection<String> parsePartsFromManifestContent(String manifestContent) {
        Node manifest = new XmlParser().parseText(manifestContent)
        Collection<String> partNames = manifest?.image?.parts?.part?.filename?.collect { it.text() }
        partNames ?: []
    }

    /**
     * Fetches the content of an S3 object as text.
     * It's important to close the input stream as soon as possible in order to avoid leaking open connections.
     * @see AmazonS3#getObject
     *
     * @param userContext who, where, why
     * @param bucket the bucket name where the object is stored
     * @param key the object key for the file
     * @return String the file contents of the specified object
     */
    private String getObjectContent(UserContext userContext, String bucket, String key) {
        String manifestContent = null
        InputStream manifestContentStream = null
        try {
            S3Object s3Object = awsClient.by(userContext.region).getObject(bucket, key)
            manifestContentStream = s3Object.objectContent
            manifestContent = manifestContentStream.text
        } catch (AmazonServiceException ase) {
            if (!(ase.errorCode in ['NoSuchBucket', 'NoSuchKey'])) {
                handleError("Unable to get S3 content from ${bucket}/${key}: ${ase}", ase)
            }
        } finally {
            manifestContentStream?.close()
        }
        return manifestContent
    }

    /**
     * Deletes a batch of objects in a bucket. Useful for deleting all the keyname.part.00 files from a bundle without
     * deleting the manifest.xml file that lists the parts.
     *
     * @param userContext who, where, why
     * @param bucket the bucket name where the bundle is stored
     * @param keys the names of the parts of the bundle
     */
    private void deleteObjectsInBucket(UserContext userContext, String bucket, Collection<String> keys) {
        for (String key in keys) {
            log.debug "Deleting S3 object ${bucket}/${key}"
            awsClient.by(userContext.region).deleteObject(new DeleteObjectRequest(bucket, key))
            Time.sleepCancellably(50) // Avoid rate limiting
        }
    }

    /**
     * Useful for verifying that all the parts of a bundle have been deleted before deleting the manifest. Any objects
     * that are not deleted will result in
     *
     * @param userContext who, where, why
     * @param bucket the bucket name where the bundle is stored
     * @param keys the names of the parts of the bundle
     * @return Boolean true if and only if all the parts have been deleted
     */
    private Boolean areAllObjectsDeleted(UserContext userContext, String bucket, Collection<String> keys) {
        log.debug "Checking for metadata for ${bucket} ${keys}"
        List<String> objectsThatStillExist = keys.findAll { doesObjectExist(userContext, bucket, it) }
        if (objectsThatStillExist.size()) {
            handleError("Bucket ${bucket} still contains: ${objectsThatStillExist}")
        }
        return objectsThatStillExist.size() == 0
    }

    /**
     * Checks to see if an S3 object exists by querying for the object's metadata and interpreting an
     * AmazonServiceException as evidence that the object does not exist.
     *
     * @param userContext who, where, why
     * @param bucket the bucket name where the object is stored
     * @param key the object key for the file
     * @return Boolean true if and only if it's possible to retrieve metadata for the S3 object
     */
    private Boolean doesObjectExist(UserContext userContext, String bucket, String key) {
        try {
            Time.sleepCancellably(50) // Avoid rate limiting
            awsClient.by(userContext.region).getObjectMetadata(bucket, key)
            return true
        } catch (AmazonServiceException ase) {
            return false
        }
    }

    /**
     * Log and email S3 errors, because S3 delete operations are rumored to be unreliable
     *
     * @param msg error message
     * @param e any existing exception
     */
    private void handleError(String msg, Exception e = null) {
        Exception exception =  e ?: new Exception(msg)
        log.error(msg, exception)
        emailerService.sendExceptionEmail(msg, exception)
    }
}
