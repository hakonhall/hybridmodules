.PHONY: hybridmodules bundle
all: hybridmodules bundle

hybridmodules:
	mvn -nsu install

bundle:
	$(MAKE) -C $@
