.PHONY:
all:
	rm -rf jlink.out
	jlink --module-path 0/jar/out.jar:A/jar/out.jar --add-modules root,java.net.http,java.logging --output jlink.out --launcher run-c5=root
	jlink.out/bin/run-c5
