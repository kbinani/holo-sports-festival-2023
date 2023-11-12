.PHONY: all
all: plugin server resourcepack

.PHONY: plugin
plugin:
	bash plugin/build.sh

.PHONY: server
server:
	bash server/build.sh

.PHONY: resourcepack
resourcepack:
	bash resourcepack/build.sh

clean:
	bash plugin/clean.sh
	bash server/clean.sh
	bash resourcepack/clean.sh

.PHONY: run
run: plugin
	rm -f ./run/plugins/sports-festival-2023.jar
	ln -sf $$(pwd)/plugin/build/libs/*.jar ./run/plugins/sports-festival-2023.jar
	rm -f ./run/server.jar
	ln -sf $$(pwd)/server/Paper/build/libs/*.jar ./run/server.jar
	cd ./run && java -jar server.jar -nogui
