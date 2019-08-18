test_directories = m1 m2 m3
target =

.PHONY: all clean $(test_directories)

all: target = all
all: $(test_directories)

$(test_directories):
	$(MAKE) -C $@ $(target)


clean: target = clean
clean: $(test_directories)
