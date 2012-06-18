/*
 * Copyright 2012 Netflix, Inc.
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
 */
package com.netflix.asgard

import org.codehaus.groovy.tools.Utilities
import javax.servlet.http.Cookie

public class IndentWriter extends PrintWriter {
    protected boolean needIndent = true
    protected String indentString
    protected int indentLevel = 0

    IndentWriter(Writer w) {
        this(w, "    ", 1, true)
    }

    IndentWriter(Writer w, String indent, int level, boolean needs) {
        super(w)
        indentString = indent
        indentLevel = level
        needIndent = needs
    }

    int getIndent() { return indentLevel }

    IndentWriter plus(int i) {
        return new IndentWriter(out, indentString, indentLevel + i, needIndent)
    }

    IndentWriter minus(int i) { return plus(-i) }

    IndentWriter next() { return plus(1) }

    IndentWriter previous() { return minus(1) }

    protected void printIndent() {
        needIndent = false
        super.print(Utilities.repeatString(indentString, indentLevel))
    }

    protected void checkIndent() {
        if (needIndent) {
            needIndent = false
            printIndent()
        }
    }

    public void println() {
        super.println()
        needIndent = true
    }

    void print(boolean b) { checkIndent(); super.print(b) }

    void print(char c) { checkIndent(); super.print(c) }

    void print(char[] s) { checkIndent(); super.print(s) }

    void print(double d) { checkIndent(); super.print(d) }

    void print(float f) { checkIndent(); super.print(f) }

    void print(int i) { checkIndent(); super.print(i) }

    void print(long l) { checkIndent(); super.print(l) }

    void print(Object obj) { checkIndent(); super.print(obj) }

    void print(String s) { checkIndent(); super.print(s) }
}

class PrettyWriter extends IndentWriter {
    PrettyWriter(Writer w) {
        super(w, ' ', 0, true)
    }

    PrettyWriter(Writer w, String ins, int level, boolean needsIt) {
        super(w, ins, level, needsIt)
    }

    PrettyWriter plus(int i) {
        return new PrettyWriter(out, indentString, indentLevel + i, needIndent)
    }

    void print(Collection list) {
        println('[')
        (this + 1).withWriter {
            indent -> list.each {
                if (it.metaClass.hasProperty(it,  "name") && it.metaClass.hasProperty(it, "value")) {
                    indent.print " ${it.name}=${it.value}"
                } else {
                    indent.print it
                }
                indent.println ';'
            }
        }
        print(']')
    }

    void println(Collection list) { print list; println() }

    void print(Map map) {
        println('[')
        (this + 1).withWriter { indent ->
            map.entrySet().each {
                indent.print it.key
                indent.print ' : '
                (indent + 1).print it.value
                indent.println()
            }
        }
        print(']')
    }

    void println(Map map) { print map; println() }

    void print(String[] strings) { print Arrays.asList(strings) }

    void println(String[] strings) { print strings; println() }

    void print(Cookie[] cookies) { print Arrays.asList(cookies) }

    void println(Cookie[] cookies) { print cookies; println() }

    void print(Object[] objects) { print Arrays.asList(objects) }

    void println(Object[] objects) { print objects; println() }
}
