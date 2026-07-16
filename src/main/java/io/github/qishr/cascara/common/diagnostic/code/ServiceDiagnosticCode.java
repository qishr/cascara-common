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


package io.github.qishr.cascara.common.diagnostic.code;

public enum ServiceDiagnosticCode implements DiagnosticCode {
    CONFIGURATION_ERROR("SPL-101", "Configuration error: {0}"),
    NOT_A_SERVICE("SPL-102", "{0} is not a ServiceProvider."),
    NOT_A_SERVICE_PROVIDER("SPL-103", "{0} is not a ServiceProvider."),

    FAILED_TO_REGISTER_MODULE("SPL-201", "Failed to register {0} module."),

    NOARGS_CONSTRUCTOR_REQUIRED("SPL-301", "Class {0} has no no-args constructor."),
    FAILED_TO_INSTANTIATE_CLASS("SPL-302", "Failed to instantiate class {0}. {1}."),
    FAILED_TO_LOAD_CLASS("SPL-304", "Failed to instantiate class {0}. {1}."),
    FAILED_TO_READ_JAR("SPL-305", "Failed to read Jar \"{0}\". {1}."),
    NON_MODULAR_JAR("SPL-306", "Jar \"{0}\" does not contain a module."),
    NO_PROVIDER_REGISTERED("SPL-307", "No {0} providers registered."),
    NO_PROVIDER_REGISTERED_FOR("SPL-301", "No {0} providers registered for {1}.");

    private final String code;
    private final String message;

    ServiceDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}