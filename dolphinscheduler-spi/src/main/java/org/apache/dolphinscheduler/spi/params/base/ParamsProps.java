/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.spi.params.base;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * the props field in form-create`s json rule
 */
public class ParamsProps {
    private PropsType propsType;

    private String placeholder;

    private int rows;

    @JsonProperty("type")
    public PropsType getPropsType() {
        return propsType;
    }

    public ParamsProps setPropsType(PropsType propsType) {
        this.propsType = propsType;
        return this;
    }

    @JsonProperty("placeholder")
    public String getPlaceholder() {
        return placeholder;
    }

    public ParamsProps setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @JsonProperty("rows")
    public int getRows() {
        return rows;
    }

    public ParamsProps setRows(int rows) {
        this.rows = rows;
        return this;
    }
}
