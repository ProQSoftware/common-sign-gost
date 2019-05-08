package ru.i_novus.common.sign.context;

/*-
 * -----------------------------------------------------------------
 * common-sign-gost
 * -----------------------------------------------------------------
 * Copyright (C) 2018 - 2019 I-Novus LLC
 * -----------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

/**
 * A NamespaceContext implementation for digital signatures
 */
public class DSNamespaceContext implements NamespaceContext {

    private Map<String, String> namespaceMap = new HashMap<>();

    public DSNamespaceContext() {
        namespaceMap.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        namespaceMap.put("dsig", "http://www.w3.org/2000/09/xmldsig#");
    }

    public DSNamespaceContext(Map<String, String> namespaces) {
        this();
        namespaceMap.putAll(namespaces);
    }

    public String getNamespaceURI(String arg0) {
        return namespaceMap.get(arg0);
    }

    public void putPrefix(String prefix, String namespace) {
        namespaceMap.put(prefix, namespace);
    }

    public String getPrefix(String arg0) {
        for (String key : namespaceMap.keySet()) {
            String value = namespaceMap.get(key);
            if (value.equals(arg0)) {
                return key;
            }
        }
        return null;
    }

    public Iterator<String> getPrefixes(String arg0) {
        return namespaceMap.keySet().iterator();
    }
}
