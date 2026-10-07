-include .env
export

ifeq (,$(wildcard .env))
$(warning No .env found; device and build targets need it: cp .env.sample .env)
endif

DEVICE ?=

PKG          := in.androidtweak.rain
SETTINGS_ACT := $(PKG)/.SettingsActivity
WALLPAPER    := $(PKG)/in.androidtweak.rain.HackerWallpaperService
ADB_BASE     := $(ANDROID_SDK)/platform-tools/adb
PHYS_DEVICE   = $(shell test -x $(ADB_BASE) && $(ADB_BASE) devices | awk -F'\t' 'NR>1 && $$2=="device" && $$1 !~ /^emulator-/ {print $$1; exit}')
ADB           = $(ADB_BASE) -s "$(or $(DEVICE),$(PHYS_DEVICE))"
GRADLEW      := JAVA_HOME="$(JAVA_HOME)" ANDROID_HOME="$(ANDROID_SDK)" ANDROID_SDK_ROOT="$(ANDROID_SDK)" ./gradlew
DEBUG_APK    := app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK  := app/build/outputs/apk/release/app-release.apk
RELEASE_AAB  := app/build/outputs/bundle/release/app-release.aab
AAPT2         = $(shell ls "$(ANDROID_SDK)/build-tools/"*/aapt2 2>/dev/null | sort | tail -1)

.PHONY: help build install run wallpaper emulator emulator-install emulator-run release release-install bundle uninstall clear-data clean logcat device-check signing-check

.DEFAULT_GOAL := help

help: ## List available commands
	@echo "Akshara Mazha - available make commands:"
	@echo
	@grep -hE '^[a-zA-Z0-9_-]+:.*## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":[^#]*## "} {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'
	@echo
	@echo "Overridable variables: DEVICE=$(if $(DEVICE),$(DEVICE),(auto)) AVD=$(AVD)"

build: ## Assemble the debug APK
	$(GRADLEW) assembleDebug

install: build device-check ## Build and install the debug APK on the device (DEVICE=<serial>)
	$(ADB) install -r $(DEBUG_APK)

run: install ## Install and open the settings screen
	$(ADB) shell am start -n $(SETTINGS_ACT)

wallpaper: install ## Install and open the live wallpaper preview to apply it
	$(ADB) shell am start -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER \
		--ecn android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT $(WALLPAPER)

emulator: ## Boot the AVD emulator (if not already running) and wait for it (AVD=<name>)
	@if $(ADB_BASE) -s emulator-5554 get-state >/dev/null 2>&1; then \
		echo "emulator-5554 already running."; \
	else \
		echo "Booting $(AVD)..."; \
		"$(ANDROID_SDK)/emulator/emulator" -avd $(AVD) -no-boot-anim >/dev/null 2>&1 & \
		echo "Waiting for boot..."; \
		$(ADB_BASE) -s emulator-5554 wait-for-device; \
		until [ "$$($(ADB_BASE) -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do \
			sleep 2; \
		done; \
		echo "$(AVD) booted."; \
	fi

emulator-install: emulator ## Boot the emulator and install the debug APK on it
	$(MAKE) install DEVICE=emulator-5554

emulator-run: emulator ## Boot the emulator, install and open the live wallpaper preview
	$(MAKE) wallpaper DEVICE=emulator-5554

# Signing config is read from .env (see the -include near the top):
#   RAIN_KEYSTORE, RAIN_KEYSTORE_PASSWORD, RAIN_KEY_ALIAS, RAIN_KEY_PASSWORD
release: signing-check ## Assemble the signed release APK (signing creds in .env)
	$(GRADLEW) assembleRelease
	@$(MAKE) --no-print-directory -s print-artifact ARTIFACT=$(RELEASE_APK)

release-install: release device-check ## Build and install the release APK
	$(ADB) install -r $(RELEASE_APK)

bundle: signing-check ## Assemble the signed release App Bundle (.aab) for Google Play
	$(GRADLEW) bundleRelease
	@$(MAKE) --no-print-directory -s print-artifact ARTIFACT=$(RELEASE_AAB)

uninstall: device-check ## Remove the app from the device
	$(ADB) uninstall $(PKG)

clear-data: device-check ## Clear the app's data on the device
	$(ADB) shell pm clear $(PKG)

clean: ## Clean the gradle build
	$(GRADLEW) clean

logcat: device-check ## Stream logcat filtered to the app's process
	@pid=$$($(ADB) shell pidof -s $(PKG) | tr -d '\r'); \
	test -n "$$pid" || { echo "$(PKG) is not running. Launch it first: make run"; exit 1; }; \
	echo "Streaming logcat for $(PKG) (pid $$pid). Ctrl-C to stop."; \
	$(ADB) logcat --pid=$$pid

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

device-check:
	@$(ADB) get-state >/dev/null 2>&1 || { \
		if [ -n "$(DEVICE)" ]; then \
			echo "Device '$(DEVICE)' not connected:"; \
		else \
			echo "No physical device connected (emulators are skipped):"; \
		fi; \
		$(ADB_BASE) devices; \
		echo "Connect a physical device, or use 'make emulator-install' / 'make emulator-run'"; \
		echo "to target the emulator. Pass DEVICE=<serial> to pick a specific device."; \
		exit 1; }
