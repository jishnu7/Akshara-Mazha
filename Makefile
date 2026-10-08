-include .env
export

ifeq (,$(wildcard .env))
$(warning No .env found; device and build targets need it: cp .env.sample .env)
endif

PKG          := in.androidtweak.rain
SETTINGS_ACT := $(PKG)/.SettingsActivity
WALLPAPER    := $(PKG)/in.androidtweak.rain.HackerWallpaperService
ADB          := $(ANDROID_SDK)/platform-tools/adb
WITH_DEVICE   = serial=$$(ANDROID_HOME="$(ANDROID_SDK)" scripts/adb-device.sh) && export ANDROID_SERIAL=$$serial &&
GRADLEW      := JAVA_HOME="$(JAVA_HOME)" ANDROID_HOME="$(ANDROID_SDK)" ANDROID_SDK_ROOT="$(ANDROID_SDK)" ./gradlew
DEBUG_APK    := app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK  := app/build/outputs/apk/release/app-release.apk
RELEASE_AAB  := app/build/outputs/bundle/release/app-release.aab
AAPT2         = $(shell ls "$(ANDROID_SDK)/build-tools/"*/aapt2 2>/dev/null | sort | tail -1)

.PHONY: help build install run wallpaper release release-install bundle uninstall clear-data clean logcat signing-check

.DEFAULT_GOAL := help

help: ## List available commands
	@echo "Mazha - available make commands:"
	@echo
	@grep -hE '^[a-zA-Z0-9_-]+:.*## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":[^#]*## "} {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

build: ## Assemble the debug APK
	$(GRADLEW) assembleDebug

install: build ## Build and install the debug APK on a device
	@$(WITH_DEVICE) $(ADB) install -r $(DEBUG_APK)

run: build ## Install and open the settings screen
	@$(WITH_DEVICE) $(ADB) install -r $(DEBUG_APK) && $(ADB) shell am start -n $(SETTINGS_ACT)

wallpaper: build ## Install and open the live wallpaper preview to apply it
	@$(WITH_DEVICE) $(ADB) install -r $(DEBUG_APK) && \
	$(ADB) shell am start -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER \
		--ecn android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT $(WALLPAPER)

# Signing config is read from .env (see the -include near the top):
#   RAIN_KEYSTORE, RAIN_KEYSTORE_PASSWORD, RAIN_KEY_ALIAS, RAIN_KEY_PASSWORD
release: signing-check ## Assemble the signed release APK (signing creds in .env)
	$(GRADLEW) assembleRelease
	@$(MAKE) --no-print-directory -s print-artifact ARTIFACT=$(RELEASE_APK)

release-install: release ## Build and install the release APK on a device
	@$(WITH_DEVICE) $(ADB) install -r $(RELEASE_APK)

bundle: signing-check ## Assemble the signed release App Bundle (.aab) for Google Play
	$(GRADLEW) bundleRelease
	@$(MAKE) --no-print-directory -s print-artifact ARTIFACT=$(RELEASE_AAB)

uninstall: ## Remove the app from a device
	@$(WITH_DEVICE) $(ADB) uninstall $(PKG)

clear-data: ## Clear the app's data on a device
	@$(WITH_DEVICE) $(ADB) shell pm clear $(PKG)

clean: ## Clean the gradle build
	$(GRADLEW) clean

logcat: ## Stream logcat filtered to the app's process
	@$(WITH_DEVICE) { pid=$$($(ADB) shell pidof -s $(PKG) | tr -d '\r'); \
	test -n "$$pid" || { echo "$(PKG) is not running. Launch it first: make run"; exit 1; }; \
	echo "Streaming logcat for $(PKG) (pid $$pid). Ctrl-C to stop."; \
	$(ADB) logcat --pid=$$pid; }

print-artifact:
	@f="$(ARTIFACT)"; \
	test -f "$$f" || { echo "$$f not found."; exit 1; }; \
	bytes=$$(stat -f%z "$$f"); \
	mb=$$(awk "BEGIN{printf \"%.2f\", $$bytes/1048576}"); \
	ver=; \
	case "$$f" in *.apk) ver=$$("$(AAPT2)" dump badging "$$f" 2>/dev/null | \
		sed -n "s/.*versionCode='\([0-9]*\)' versionName='\([^']*\)'.*/v\2 (build \1)/p");; esac; \
	echo; printf "  %s  %s MB (%s bytes)  %s\n" "$$f" "$$mb" "$$bytes" "$$ver"

signing-check:
	@test -n "$$RAIN_KEYSTORE" -a -f "$$RAIN_KEYSTORE" || { \
		echo "Release signing not configured. Set RAIN_KEYSTORE* in .env (see .env.sample)."; \
		exit 1; }
