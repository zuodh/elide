/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.embeddedid;

import com.yahoo.elide.annotation.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Include(rootLevel = true)
@Data
@Entity
public class Building {
    @Id
    Address address;

    String name;

    @OneToMany
    @EqualsAndHashCode.Exclude
    Set<Building> neighbors;
}
