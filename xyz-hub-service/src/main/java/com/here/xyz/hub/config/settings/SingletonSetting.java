/*
 * Copyright (C) 2017-2023 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.xyz.hub.config.settings;

import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * A setting which may only exist ones for the whole service and also only once inside the settings config table.
 * The ID of a SingletonSetting is always its class name.
 */
@JsonSubTypes({
    @JsonSubTypes.Type(value = EnvironmentVariableOverrides.class),
    @JsonSubTypes.Type(value = SpaceStorageMatchingMap.class)
})
public abstract class SingletonSetting<T> extends Setting<T> {

  public SingletonSetting() {
    this.id = getClass().getSimpleName();
  }

}
