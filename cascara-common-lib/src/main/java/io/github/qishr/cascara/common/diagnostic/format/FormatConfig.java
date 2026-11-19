// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package io.github.qishr.cascara.common.diagnostic.format;

import io.github.qishr.cascara.common.annotation.Experimental;

@Experimental
public class FormatConfig {
    private boolean prefixEveryLine = true;
    private boolean includeStackTrace = false;
    private boolean includeProblemCodes = false;
    private boolean incluedTime = true;
    private boolean incluedPid = true;
    private boolean incluedThread = true;
    private boolean incluedSource = true;

    private boolean useSimpleClassName = false;

    public FormatConfig() {}

    public boolean prefixEveryLine() {
        return prefixEveryLine;
    }

    public FormatConfig setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return this;
    }

    public boolean includeStackTrace() {
        return includeStackTrace;
    }

    public FormatConfig setIncludeStackTrace(boolean b) {
        includeStackTrace = b;
        return this;
    }

    public boolean includeProblemCodes() {
        return includeProblemCodes;
    }

    public FormatConfig setIncludeProblemCodes(boolean b) {
        includeProblemCodes = b;
        return this;
    }

    public boolean incluedTime() {
        return incluedTime;
    }

    public FormatConfig setIncludeTime(boolean b) {
        incluedTime = b;
        return this;
    }

    public boolean incluedPid() {
        return incluedPid;
    }

    public FormatConfig setIncluedPid(boolean b) {
        incluedPid = b;
        return this;
    }

    public boolean incluedThread() {
        return incluedThread;
    }

    public FormatConfig setIncluedThread(boolean b) {
        incluedThread = b;
        return this;
    }

    public boolean incluedSource() {
        return incluedSource;
    }

    public FormatConfig setIncluedSource(boolean b) {
        incluedSource = b;
        return this;
    }

    public boolean useSimpleClassName() {
        return useSimpleClassName;
    }

    public FormatConfig setUseSimpleClassName(boolean b) {
        useSimpleClassName = b;
        return this;
    }
}
