makeFileDir := $(abspath $(dir $(abspath $(lastword $(MAKEFILE_LIST)))))

init:
	pip install poetry

rsync-rfcs:
	nice -n 19 rsync -avz --delete --exclude '*.pdf' --exclude '*.ps' --exclude 'tar' \
		ftp.rfc-editor.org::rfc-ed-all $(makeFileDir)/rv-downloader/rsync

preprocess-rfcs:
	cd $(makeFileDir)/rv-downloader && \
		poetry run python preprocess_rfcs.py

create-index:
	cd $(makeFileDir)/rv-searcher && \
		./gradlew createIndex --args='$(makeFileDir)/rv-downloader/preprocessed_rfcs.jsonl'
