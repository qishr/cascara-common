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

import java.util.HashSet;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.code.ServiceDiagnosticCode;
import io.github.qishr.cascara.common.service.ServiceProviderKernel;
import io.github.qishr.cascara.common.trackable.TrackableArray;
import io.github.qishr.cascara.common.util.ContentType;

public class ServiceProviderRoot extends ServiceProviderBranch implements ServiceProviderKernel {
    Set<ContentType> contentTypes = new HashSet<>();
    TrackableArray<String> userModules = new TrackableArray<>();

    private ServiceProviderRoot() {}

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured.
    public static ServiceProviderKernel instance() {
        return instance(null);
    }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured with a specified Reporter.
    /// This reporter is used for non-fatal error and warning reporting.
    public static ServiceProviderKernel instance(Reporter reporter) {
        if (reporter == null) {
            reporter = new NoOpReporter();
        }
        if (rootLayer == null) {
            final Reporter bootReporter = reporter;
            rootLayer = new ServiceProviderRoot();
            rootLayer.name = "root";
            rootLayer.setReporter(reporter);
            ModuleLayer boot = ModuleLayer.boot();
            boot.modules().forEach((module) -> {
                final String moduleName = module.getName();
                try {
                    bootReporter.trace("Found module " + moduleName);
                    rootLayer.registerModule(module);
                } catch (Exception e) {
                    bootError(e,
                        ServiceDiagnosticCode.FAILED_TO_REGISTER_MODULE,
                        moduleName);
                }
            });
            // Fallback: classic ServiceLoader scanning for classpath/unnamed-module usage,
            // and to pick up any providers using META-INF/services even when modular.
            rootLayer.registerViaServiceLoader();
        }
        return rootLayer;
    }
}
