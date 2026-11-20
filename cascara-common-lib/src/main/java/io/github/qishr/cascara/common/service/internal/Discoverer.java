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

package io.github.qishr.cascara.common.service.internal;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ClassPath;
import io.github.qishr.cascara.common.util.ModulePath;
import io.github.qishr.cascara.common.util.ModulePath.DiscoveredModule;

public class Discoverer {
    public static Set<DiscoveredModule> discoverModulesAndClasses(Set<String> excludePackages) {
        // Discover classes on the module path
        ModulePath modulePath = new ModulePath(null, excludePackages, Cascara.get().getModulePath());
        Set<DiscoveredModule> modules = modulePath.getDiscoveredModules();

        // Get the set of modulepath entries
        Set<String> modulePathSet = pathSet(System.getProperty("jdk.module.path"));

        // Get the set of classpath entries
        Set<String> classPathSet = pathSet(System.getProperty("java.class.path"));

        // Remove entries from the classpath set that are covered by the modulepath
        classPathSet.removeAll(modulePathSet);

        // Discover classes in the abridged classpath set
        ClassPath classPath = new ClassPath(classPathSet, excludePackages, Cascara.get().getModulePath());
        Set<DiscoveredModule> cpModules = classPath.getDiscoveredModules();

        modules.addAll(cpModules);

        return modules;
    }

    private static Set<String> pathSet(String paths) {
        Set<String> pathSet = new HashSet<>();
        if (paths != null) {
            Collections.addAll(pathSet, paths.split(File.pathSeparator, -1));
        }
        return pathSet;
    }
}
