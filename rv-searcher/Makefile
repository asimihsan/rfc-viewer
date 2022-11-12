makeFileDir := $(abspath $(dir $(abspath $(lastword $(MAKEFILE_LIST)))))

native-docker-build:
	docker buildx build -t rv-searcher-native -f Dockerfile.native .

native-docker-shell: native-docker-build
	docker run -it rv-searcher-native /bin/bash

native-jar: native-docker-build
	docker run -v $(makeFileDir):/workdir --platform linux/arm64 -it rv-searcher-native \
		/bin/bash -c 'cd /workdir && /root/.sdkman/candidates/gradle/current/bin/gradle shadowJar'

native-create-index: native-jar
	docker run -v $(makeFileDir):/workdir --platform linux/arm64 -it rv-searcher-native \
		/bin/bash -c 'cd /workdir && native-image \
				--no-fallback \
				-cp /workdir/build/libs/rv-searcher-unspecified.jar \
				-H:Class=art.kittencat.CreateIndex \
				-H:+ReportUnsupportedElementsAtRuntime \
				--features=art.kittencat.ReflectionRegistration'

native-lambda-runtime: native-jar
	docker run -v $(makeFileDir):/workdir --platform linux/arm64 -it rv-searcher-native \
		/bin/bash -c 'cd /workdir && native-image \
				--no-fallback \
				-cp /workdir/build/libs/rv-searcher-unspecified.jar \
				-H:Class=art.kittencat.lambda.LambdaRuntime \
				-H:+ReportUnsupportedElementsAtRuntime \
				--features=art.kittencat.ReflectionRegistration'

native-create-index-run:
	docker run -v $(makeFileDir):/workdir -v $(abspath $(makeFileDir)/../rv-downloader):/indexdir \
		--platform linux/arm64 -it rv-searcher-native \
			/bin/bash -c 'cd /workdir && /workdir/art.kittencat.createindex /indexdir/preprocessed_rfcs.jsonl'
