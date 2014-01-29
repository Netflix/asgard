/*
 * Copyright 2013 Netflix, Inc.
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


import com.netflix.asgard.navigator.EmptyNavigator
import com.netflix.asgard.navigator.NonEmptyNavigator
import geb.Browser
import org.apache.tools.ant.taskdefs.condition.Os
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver

baseUrl = System.properties.'geb.build.baseUrl' ?: 'http://localhost:8080/'
reportsDir = "target/reports"

driver = {
    def driverOs = Os.isFamily(Os.FAMILY_MAC) ? 'mac' : 'linux'
    System.setProperty('webdriver.chrome.driver', new File("test/functional/drivers/chrome/${driverOs}/chromedriver").absolutePath)
    new ChromeDriver()
}

environments {
    sauce {
        String username = System.properties.'saucelabs.username'
        String apiKey = System.properties.'saucelabs.key'

        if (username == null || apiKey == null) {
            System.err.println("Sauce OnDemand credentials not set.")
        }

        DesiredCapabilities caps = DesiredCapabilities.chrome()
        caps.setCapability("platform", 'OS X 10.8')
        caps.setCapability("version", '')
        caps.setCapability("name", 'asgard')

        driver = {
            new RemoteWebDriver(new URL("http://${username}:${apiKey}@ondemand.saucelabs.com:80/wd/hub"), caps)
        }
    }
}

innerNavigatorFactory = { Browser browser, List<WebElement> elements ->
    elements ? new NonEmptyNavigator(browser, elements) : new EmptyNavigator(browser)
}