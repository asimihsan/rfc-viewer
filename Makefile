makeFileDir := $(abspath $(dir $(abspath $(lastword $(MAKEFILE_LIST)))))

init:
	pip install poetry

rsync-rfcs:
	nice -n 19 rsync -avz --delete --exclude '*.pdf' --exclude '*.ps' --exclude 'tar' --no-links \
		ftp.rfc-editor.org::rfc-ed-all $(makeFileDir)/rv-downloader/rsync

preprocess-rfcs:
	cd $(makeFileDir)/rv-downloader && \
		poetry run python preprocess_rfcs.py

create-index:
	cd $(makeFileDir)/rv-searcher && \
		./gradlew createIndex --args='$(makeFileDir)/rv-downloader/preprocessed_rfcs.jsonl'

graalvm-create-index:
	cd $(makeFileDir)/rv-searcher && \
		./gradlew shadowJar && \
		native-image \
			--no-fallback \
			-cp ./build/libs/rv-searcher-unspecified.jar \
			-H:Class=art.kittencat.CreateIndex \
			-H:+ReportUnsupportedElementsAtRuntime \
			--features=art.kittencat.ReflectionRegistration && \
		./art.kittencat.createindex '/Users/asimi/workplace/rfc-viewer/rv-downloader/preprocessed_rfcs.jsonl'

graalvm-query-example:
	cd $(makeFileDir)/rv-searcher && \
		./gradlew shadowJar && \
		native-image \
			--no-fallback \
			-cp ./build/libs/rv-searcher-unspecified.jar \
			-H:Class=art.kittencat.ExampleQuery \
			-H:+ReportUnsupportedElementsAtRuntime \
			--features=art.kittencat.ReflectionRegistration

searcher:
	cd $(makeFileDir)/rv-searcher && \
        ./gradlew build && \
		./gradlew copyRuntimeDependencies && \
		./gradlew shadowJar

searcher-lambda-docker-build: searcher
	cd $(makeFileDir)/rv-searcher && \
		docker buildx build -t rv-searcher .

searcher-lambda-run-docker: searcher-lambda-docker-build
	cd $(makeFileDir)/rv-searcher && \
		docker run -p 9000:9000 rv-searcher

searcher-lambda-docker-build-native:
	cd $(makeFileDir)/rv-searcher && \
		docker buildx build -t rv-searcher-native -f Dockerfile.native .

build-web:
    # TODO lookup using CloudFormation stack output variable
	cd $(makeFileDir)/rv-web && npm run build

deploy-cdk: searcher-lambda-docker-build build-web
	cd $(makeFileDir)/rv-cdk && \
		(source ~/.aws_kitten_cat_credentials && cdk deploy)
