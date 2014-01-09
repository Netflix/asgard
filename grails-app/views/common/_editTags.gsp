<tr class="prop">
	<td class="name">Tags:</td>
	<td>

		<div class="list">
			<table id="tags">
				<thead>
					<tr>
						<th>Key</th>
						<th>Value</th>
						<th>Prop</th>
						<th>Delete?</th>
					</tr>
				</thead>
				<g:each var="tag" in="${tags}" status="i">
					<tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
						<td>
							${tag.key}<input type="hidden" id="tags.name.${tag.key}"
							class="tagName" value="${tag.key}" />
						</td>
						<td><input type="text" id="tags.value.${tag.key}"
							name="tags.value.${tag.key}" class="tagValue"
							value="${tag.value}" required
							onchange="uniqueValue(jQuery(this))" /></td>
						<g:if test="${tag.propagateAtLaunch }">
							<td><input type="checkbox" id="tags.props.${tag.key}"
								name="tags.props.${tag.key}" checked="yes" /></td>
						</g:if>
						<g:else>
							<td><input type="checkbox" id="tags.props.${tag.key}"
								name="tags.props.${tag.key}" /></td>
						</g:else>
						<td><input type="button" class="ibtnDel"  value="Delete">
						<!-- <input type="checkbox" class="tagsDel"
							id="tags.delete.${tag.key }" name="tags.delete.${tag.key }" /></td> -->
					</tr>
				</g:each>
			</table>
			<input type="button" id="addrow" value="Add Tag" />
		</div>
	</td>
</tr>
