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


/// Cascara Service Provider Layer (SPL)
///
/// Method names
///
/// Methods in the [ServiceProviderLayer] and [ServiceProviderRoot] interfaces
/// are split into categories:
///
/// **Find in All Layers**
///
/// The *find* methods return a service provider or list of providers from the
/// SPL hierarchy.
/// They start at the layer they're called from, search all layers up to the root layer,
/// then search branch layers. The first matching provider is returned.
///
/// - `ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType)`
/// - `ServiceMetadata findProvider(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate)`
/// - `Set<Class<ServiceProvider>> findServiceTypes()`
/// - `Set<ServiceMetadata> findServices()`
/// - `List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType)`
/// - `List<ServiceMetadata> findAllProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate)`
///
/// **Get from Specific Layer**
///
/// The *get* methods return a service provider or list of service providers from the current layer.
///
/// - `ServiceMetadata getProvider(String providerName)`
/// - `Collection<ServiceMetadata> getProviders()`
/// - `Collection<ServiceMetadata> getProvidersByFqcn()`
/// - `List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType)`
/// - `List<ServiceMetadata> getProviders(Class<? extends ServiceProvider> serviceType, Predicate<ServiceMetadata> capabilityPredicate)`
///
/// - `boolean hasProvider(String name)`
///
/// **Layer metadata, hierarchy, creation and deletion**
///
/// **Provider Registration in Specific Layer**
///
/// **Load**
///
/// These static methods return an instance of a serive provider.
///
/// - `<T> loadProvider(Class<T> serviceType, ServiceMetadata metadata)`
/// - `<T> loadDefault(Class<T> serviceType)`
///
/// **Instantiate**
///
/// A convenience method for instantiating provider classes.
///
/// - `instantiateProvider(Class<T> providerClass)`
///
package io.github.qishr.cascara.common.service;
