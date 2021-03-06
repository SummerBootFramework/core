/*
 * Copyright 2005 The Summer Boot Framework Project
 *
 * The Summer Boot Framework Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.summerframework.boot;

/**
 *
 * @author Changski Tie Zheng Zhang, Du Xiao
 */
public interface BootPOI {

    //SummerBoot Lifecycle
    String SERVICE_BEGIN = "service.begin";
    String AUTH_BEGIN = "auth.begin";
    String PROCESS_BEGIN = "process.begin";
    String BIZ_BEGIN = "biz.begin";
    String BIZ_END = "biz.end";
    String PROCESS_END = "process.end";
    String SERVICE_END = "service.end";

    //Integrations
    String LDAP_BEGIN = "ldap.begin";
    String LDAP_END = "ldap.end";
    String DB_BEGIN = "db.begin";
    String DB_END = "db.end";
    String CACHE_BEGIN = "cache.begin";
    String CACHE_END = "cache.end";
    
    String RPC_BEGIN = "rpc.begin";
    String RPC_END = "rpc.end";
}
