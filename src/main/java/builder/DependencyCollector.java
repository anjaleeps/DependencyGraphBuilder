/*
 * Copyright (c)  2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package builder;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects the dependent classes used by a particular class
 */
public class DependencyCollector {

    final Pattern pattern;
    private Set<String> dependencies = new HashSet<>();

    public DependencyCollector() {
        pattern = Pattern.compile("([a-zA-Z]\\w+/)+(\\w|[$])+");
    }

    /**
     * Get the class name corresponding to the Type object passed
     */
    public void addType(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                addType(type.getElementType());
                break;
            case Type.OBJECT:
                addName(type.getInternalName());
                break;
            case Type.METHOD:
                addMethodDesc(type.getDescriptor());
                break;
        }
    }

    /**
     * Get the class name corresponding to a field, annotation, or variable description
     */
    public void addDesc(String desc) {
        addType(Type.getType(desc));
    }

    /**
     * Get the types of the classes used in a class, method, field signature
     */
    public void addSignature(String signature) {
        if (signature != null) {

            new SignatureReader(signature).accept(new SignatureNodeVisitor(this));
        }
    }

    public void addTypeSignature(String signature) {
        if (signature != null) {
            new SignatureReader(signature).acceptType(new SignatureNodeVisitor(this));
        }
    }

    /**
     * Get the class types of passed constants
     */
    public void addConstant(Object constant) {
        if (constant instanceof Type) {
            addType((Type) constant);
        } else if (constant instanceof Handle) {
            Handle handle = (Handle) constant;
            addInternalName(handle.getOwner());
            addMethodDesc(handle.getDesc());
        } else if (constant instanceof String) {
            String s = (String) constant;
            //if the passed string matches the patters of a class name, add it as a dependency
            if (checkStringConstant(s.replace('.', '/'))) {
                addInternalName(s.replace('.', '/'));
            }
        }
    }

    /**
     * Get the parameter types and return type of a method using method description
     */
    public void addMethodDesc(String desc) {
        addType(Type.getReturnType(desc));
        Type[] types = Type.getArgumentTypes(desc);
        for (int i = 0; i < types.length; i++) {
            addType(types[i]);
        }
    }

    /**
     * Get the Type of the class represented by the given class name
     */
    public void addInternalName(String name) {
        addType(Type.getObjectType(name));
    }

    public void addInternalNames(String[] names) {
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                addInternalName(names[i]);
            }
        }
    }

    /**
     * Get the ClassGraphNode for the given class name from builder and add it as a
     * class dependency of the current class. Only those that have created nodes are added.
     */
    public void addName(String name) {
        dependencies.add(name);
    }

    /**
     * Check if a string passed with ldc command matches the pattern of a class name.
     * Purpose is catching classes passed through reflection
     */
    public boolean checkStringConstant(String s) {
        Matcher matcher = pattern.matcher(s);
        if (matcher.matches()) {
            return true;
        }
        return false;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }
}
