package org.lite.mesh.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class EurekaInstance implements Serializable {
    String id;
    String name;
    String status;
}