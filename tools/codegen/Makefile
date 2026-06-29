# Solra 代码生成器 Makefile
# Makefile 目标作为 codegen.py 的便捷入口

ROOT := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))/../..

.PHONY: all java python typescript cpp kotlin swift clean help

all: java python typescript cpp kotlin swift

java:
	python $(ROOT)/tools/codegen/codegen.py -l java

python:
	python $(ROOT)/tools/codegen/codegen.py -l python

typescript:
	python $(ROOT)/tools/codegen/codegen.py -l typescript

cpp:
	python $(ROOT)/tools/codegen/codegen.py -l cpp

kotlin:
	python $(ROOT)/tools/codegen/codegen.py -l kotlin

swift:
	python $(ROOT)/tools/codegen/codegen.py -l swift

# 仅核心域 (P0)
core:
	python $(ROOT)/tools/codegen/codegen.py -d common avt spc auth saf

# P1 支撑域
support:
	python $(ROOT)/tools/codegen/codegen.py -d crt soc grw not

# 商业化域
commerce:
	python $(ROOT)/tools/codegen/codegen.py -d mon

clean:
	rm -rf $(ROOT)/generated/*
	@echo "✅ generated/ 已清理"

help:
	@echo "Solra 代码生成器"
	@echo ""
	@echo "  make all        — 生成全部 6 中语言"
	@echo "  make java       — 仅 Java"
	@echo "  make python     — 仅 Python"
	@echo "  make typescript — 仅 TypeScript"
	@echo "  make cpp        — 仅 C++"
	@echo "  make kotlin     — 仅 Kotlin"
	@echo "  make swift      — 仅 Swift"
	@echo "  make core       — 仅 P0 核心域"
	@echo "  make support    — 仅 P1 支撑域"
	@echo "  make commerce   — 仅 P2 商业化域"
	@echo "  make clean      — 清理生成目录"
