plutil -insert  -xml ' 
<dict>
	<key>compileBitcode</key>
	<true/>
	<key>method</key>
	<string>app-store</string>
	<key>provisioningProfiles</key>
	<dict>
	    <key>$APP_ID</key>
	    <string>$UUID_PROVISION</string>
	</dict>
	<key>signingStyle</key>
	<string>manual</string>
	<key>teamID</key>
	<string>$DEVELOPMENT_TEAM</string>    
</dict>
</plist>' --  ${APP_NAME}/${APP_NAME}-Info.plist
