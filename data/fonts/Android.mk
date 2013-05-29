# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

copy_from :=                \
    AndroidClock.ttf \
    AndroidClock_Highlight.ttf \
    AndroidClock_Solid.ttf \
    DroidKufi-Bold.ttf \
    DroidKufi-Regular.ttf \
    DroidNaskh-Bold.ttf \
    DroidNaskh-Regular-SystemUI.ttf \
    DroidNaskh-Regular.ttf \
    DroidSansArabic.ttf \
    DroidSansArmenian.ttf \
    DroidSansDevanagari-Regular.ttf \
    DroidSansEthiopic-Bold.ttf \
    DroidSansEthiopic-Regular.ttf \
    DroidSansFallback.ttf \
    DroidSansFallbackFull.ttf \
    DroidSansFallbackLegacy.ttf \
    DroidSansGeorgian.ttf \
    DroidSansHebrew-Bold.ttf \
    DroidSansHebrew-Regular.ttf \
    DroidSansJapanese.ttf \
    DroidSansMono.ttf \
    DroidSansTamil-Bold.ttf \
    DroidSansTamil-Regular.ttf \
    DroidSansThai.ttf \
    DroidSerif-Bold.ttf \
    DroidSerif-BoldItalic.ttf \
    DroidSerif-Italic.ttf \
    DroidSerif-Regular.ttf \
    MTLc3m.ttf \
    MTLmr3m.ttf \
    Roboto-Bold.ttf \
    Roboto-BoldItalic.ttf \
    Roboto-Italic.ttf \
    Roboto-Light.ttf \
    Roboto-LightItalic.ttf \
    Roboto-Regular.ttf \
    Roboto-Thin.ttf \
    Roboto-ThinItalic.ttf \
    RobotoCondensed-Bold.ttf \
    RobotoCondensed-BoldItalic.ttf \
    RobotoCondensed-Italic.ttf \
    RobotoCondensed-Regular.ttf \

ifneq ($(NO_FALLBACK_FONT),true)
ifeq ($(filter %system/fonts/DroidSansFallback.ttf,$(PRODUCT_COPY_FILES)),)
    # if the product makefile has set the the fallback font, don't override it.
    copy_from += DroidSansFallback.ttf
endif
endif

copy_file_pairs := $(foreach cf,$(copy_from),$(LOCAL_PATH)/$(cf):system/fonts/$(cf))
PRODUCT_COPY_FILES += $(copy_file_pairs)
