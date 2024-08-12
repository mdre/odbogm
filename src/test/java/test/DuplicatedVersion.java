/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test;

import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Version;

/**
 *
 * @author mdre
 */
@Entity
public class DuplicatedVersion {
            @Version int v1;
            @Version Integer v2;
        }