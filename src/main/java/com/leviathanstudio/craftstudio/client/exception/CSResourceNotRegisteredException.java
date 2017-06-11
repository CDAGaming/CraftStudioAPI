package com.leviathanstudio.craftstudio.client.exception;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Exception raised when the program try to call a resource that isn't registered. 
 * 
 * @author Timmypote
 */
@SideOnly(Side.CLIENT)
public class CSResourceNotRegisteredException extends RuntimeException
{
    private static final long serialVersionUID = -3495512420502365486L;

    /**
     * Create an exception for a resource not registered.
     * 
     * @param resourceNameIn The resource that isn't registered.
     */
    public CSResourceNotRegisteredException(String resourceNameIn) {
        super("You are trying to acces \"" + resourceNameIn + "\", but it's not registered");
    }
}