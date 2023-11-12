.PHONY: plugin
plugin:
	cd plugin && ./gradlew assemble

.PHONY: server
server:
	cd server && ./gradlew rebuildPatches && ./gradlew applyPatches && ./gradlew createReobfBundlerJar

clean:
	rm -rf ./plugin/build/libs/*.jar

.PHONY: run
run: plugin server
	rm -f ../holo-sports-festival-2023-work/run/plugins/sports-festival-2023.jar
	ln -sf $$(pwd)/plugin/build/libs/*.jar ../holo-sports-festival-2023-work/run/plugins/sports-festival-2023.jar
	cd ../holo-sports-festival-2023-work/run && java -jar server.jar -nogui
