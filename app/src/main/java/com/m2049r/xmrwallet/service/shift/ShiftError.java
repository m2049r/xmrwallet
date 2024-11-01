/*
 * Copyright (c) 2017-2021 m2049r et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.service.shift;

import androidx.annotation.NonNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShiftError {
    @Getter
    private final Type type;
    @Getter
    private final String errorMsg;

    public enum Type {
        SERVICE,
        INFRASTRUCTURE
    }

    public boolean isRetryable() {
        return type == Type.INFRASTRUCTURE;
    }

    @Override
    @NonNull
    public String toString() {
        return getErrorMsg();
    }
}
