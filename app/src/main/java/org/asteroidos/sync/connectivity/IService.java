/*
 * AsteroidOSSync
 * Copyright (c) 2023 AsteroidOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.asteroidos.sync.connectivity;

/**
 * Every Service of Sync has to implement the {@link IService} interface.
 * A service is a module that either processes information that is exchanged
 * with the watch or is interested in knowing when the watch is connected and
 * when the watch is disconnected,
 * ({@link IService#sync()}) is called when the watch is connected.
 * ({@link IService#unsync()}) is called when the watch is disconnected.
 */
public interface IService {
    public void sync();
    public void unsync();
}
