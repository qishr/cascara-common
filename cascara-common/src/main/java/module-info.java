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

/// Cascara common APIs and utilities.
module cascara.common {
    requires java.management;
    requires java.base;
    requires jdk.unsupported;

    uses io.github.qishr.cascara.common.service.ServiceProvider;

    exports io.github.qishr.cascara.common.annotation;
    exports io.github.qishr.cascara.common.color;
    exports io.github.qishr.cascara.common.data;
    exports io.github.qishr.cascara.common.diagnostic;
    exports io.github.qishr.cascara.common.diagnostic.exception;
    exports io.github.qishr.cascara.common.diagnostic.format;
    exports io.github.qishr.cascara.common.diagnostic.log;
    exports io.github.qishr.cascara.common.diagnostic.message;
    exports io.github.qishr.cascara.common.diagnostic.report;
    exports io.github.qishr.cascara.common.exec;
    exports io.github.qishr.cascara.common.exec.ipc;
    exports io.github.qishr.cascara.common.filewatcher;
    exports io.github.qishr.cascara.common.lang.plain;
    exports io.github.qishr.cascara.common.lang.ast;
    exports io.github.qishr.cascara.common.lang.streaming;
    exports io.github.qishr.cascara.common.lang.diagnostic;
    exports io.github.qishr.cascara.common.lang.processor;
    exports io.github.qishr.cascara.common.lang.semantic;
    exports io.github.qishr.cascara.common.lang.token;
    exports io.github.qishr.cascara.common.lang.util;
    exports io.github.qishr.cascara.common.property;
    exports io.github.qishr.cascara.common.semver;
    exports io.github.qishr.cascara.common.service;
    exports io.github.qishr.cascara.common.trackable;
    exports io.github.qishr.cascara.common.trackable.property;
    exports io.github.qishr.cascara.common.trackable.tracker;
    exports io.github.qishr.cascara.common.type;
    exports io.github.qishr.cascara.common.type.cascara;
    exports io.github.qishr.cascara.common.type.lang;
    exports io.github.qishr.cascara.common.type.net;
    exports io.github.qishr.cascara.common.type.nio;
    exports io.github.qishr.cascara.common.type.time;
    exports io.github.qishr.cascara.common.type.util;
    exports io.github.qishr.cascara.common.util;

    opens io.github.qishr.cascara.common.data;           // For tests
    opens io.github.qishr.cascara.common.diagnostic;     // For tests and SchmeaGenerator
    opens io.github.qishr.cascara.common.lang.plain;     // For tests
    opens io.github.qishr.cascara.common.lang.processor; // For tests
    opens io.github.qishr.cascara.common.property;       // For tests and SchmeaGenerator
    opens io.github.qishr.cascara.common.trackable;      // For tests and SchmeaGenerator
    opens io.github.qishr.cascara.common.util;           // For tests and SchmeaGenerator

    provides io.github.qishr.cascara.common.service.ServiceProvider
        with io.github.qishr.cascara.common.type.cascara.DiagnosticMessageDescriptor,
             io.github.qishr.cascara.common.type.lang.ByteArrayDescriptor,
             io.github.qishr.cascara.common.type.lang.ByteReferenceArrayDescriptor,
             io.github.qishr.cascara.common.type.lang.CharacterArrayDescriptor,
             io.github.qishr.cascara.common.type.lang.CharArrayDescriptor,
             io.github.qishr.cascara.common.type.lang.ConfigurationSerializer,
             io.github.qishr.cascara.common.type.lang.ProcessHandleInfoSerializer,
             io.github.qishr.cascara.common.type.lang.RuntimeVersionDescriptor,
             io.github.qishr.cascara.common.type.lang.ServicesCatalogSerializer,
             io.github.qishr.cascara.common.type.lang.StackTraceElementSerializer,
             io.github.qishr.cascara.common.type.lang.ThrowableSerializer,
             io.github.qishr.cascara.common.type.net.HttpCookieDescriptor,
             io.github.qishr.cascara.common.type.net.InetAddressDescriptor,
             io.github.qishr.cascara.common.type.net.UriDescriptor,
             io.github.qishr.cascara.common.type.net.UrlDescriptor,
             io.github.qishr.cascara.common.type.nio.PathDescriptor,
             io.github.qishr.cascara.common.type.time.DurationDescriptor,
             io.github.qishr.cascara.common.type.time.InstantDescriptor,
             io.github.qishr.cascara.common.type.time.LocalDateTimeDescriptor,
             io.github.qishr.cascara.common.type.time.OffsetDateTimeDescriptor,
             io.github.qishr.cascara.common.type.time.OffsetTimeDescriptor,
             io.github.qishr.cascara.common.type.time.PeriodDescriptor,
             io.github.qishr.cascara.common.type.time.ZonedDateTimeDescriptor,
             io.github.qishr.cascara.common.type.time.ZoneIdDescriptor,
             io.github.qishr.cascara.common.type.time.ZoneOffsetDescriptor,
             io.github.qishr.cascara.common.type.util.CurrencyDescriptor,
             io.github.qishr.cascara.common.type.util.DateDescriptor,
             io.github.qishr.cascara.common.type.util.EnumMapSerializer,
             io.github.qishr.cascara.common.type.util.EnumSetSerializer,
             io.github.qishr.cascara.common.type.util.SimpleTimeZoneDescriptor,
             io.github.qishr.cascara.common.type.util.TimeZoneDescriptor,
             io.github.qishr.cascara.common.type.util.UuidDescriptor,
             io.github.qishr.cascara.common.lang.util.SourceStringBuffer,
             io.github.qishr.cascara.common.lang.util.SourceInputStreamBuffer;
}
