# Rules for make to build MathConverter
#
# Targets:
#  clean - removes all .class files
#  all   - build all classes (default)
#

SHELL=/bin/bash

ifeq ($(TOPDIR),)
export TOPDIR=$(shell pwd)
endif

JAVAC = /lyryx/enabler/system/jdk8/bin/javac
CLASSPATH := $(TOPDIR):/lyryx/enabler/java/lib:/lyryx/enabler/system/jakarta-commons/commons-io-1.3.1.jar


# Do not edit anything below this line

CLASSFILES=$(patsubst %.java,%.class,$(wildcard *.java))

SUBDIRS=$(shell find . -mindepth 1 -maxdepth 1 -follow -type d -not -name CVS -not -name test)


.PHONY: all
all: $(CLASSFILES) subdirs

.PHONY: clean
clean: subdirs
ifneq ($(strip $(CLASSFILES)),)
	rm -f $(CLASSFILES)
endif

.PHONY: subdirs $(SUBDIRS)
subdirs: $(SUBDIRS)

$(SUBDIRS):
	@if [[ ! -f $@/Makefile ]]; then \
		$(MAKE) -C $@ -f $(TOPDIR)/rules.mk $(MAKECMDGOALS); \
	else \
		$(MAKE) -C $@ $(MAKECMDGOALS); \
	fi

%.class: %.java
	$(JAVAC) -cp "$(CLASSPATH)" $<
