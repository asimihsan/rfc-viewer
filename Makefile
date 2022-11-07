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

searcher:
	cd $(makeFileDir)/rv-searcher && \
        ./gradlew build && \
		./gradlew copyRuntimeDependencies && \
		./gradlew shadowJar

searcher-lambda-docker-build: searcher
	cd $(makeFileDir)/rv-searcher && \
		docker build -t rv-searcher .

searcher-lambda-run-docker: searcher-lambda-docker-build
	cd $(makeFileDir)/rv-searcher && \
		docker run -p 9000:9000 rv-searcher

deploy-cdk: searcher-lambda-docker-build
	cd $(makeFileDir)/rv-cdk && \
		(source ~/.aws_kitten_cat_credentials && cdk deploy)
