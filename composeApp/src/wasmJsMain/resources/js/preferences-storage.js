/*
 * This file is part of Fhraise.
 * Copyright (c) 2024 HSAS Foodies. All Rights Reserved.
 *
 * Fhraise is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Fhraise is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Fhraise. If not, see <https://www.gnu.org/licenses/>.
 */

const PreferenceType = {
  INT: 0, DOUBLE: 1, STRING: 2, BOOLEAN: 3, FLOAT: 4, LONG: 5, STRING_SET: 6, BYTE_ARRAY: 7,
};

class Preference {
  constructor(key, value, type) {
    this.key = key;
    this.value = value;
    this.type = type;
  }
}

class PreferencesStorage {
  eventName = "preferencesChanged";
  preferences;
  subscription;

  constructor(name, version) {
    this.db = new Dexie(name);
    this.db.version(version).stores({
      preferences: "key, value, type",
    });

    const event = new Event(this.eventName);

    const observable = Dexie.liveQuery(() => this.db.preferences.toArray());
    this.subscription = observable.subscribe(async (preferences) => {
      this.preferences = preferences;
      document.dispatchEvent(event);
    });
  }

  async write(preferencesArray) {
    this.preferences = preferencesArray;
    try {
      await this.db.preferences.bulkUpdate(preferencesArray.map((preference) => {
        return { key: preference.key, changes: { value: preference.value, type: preference.type } };
      }));
    } catch (e) {
      console.error("Error writing preferences", e);
    }
  }

  unsubscribe() {
    this.subscription.unsubscribe();
  }
}
