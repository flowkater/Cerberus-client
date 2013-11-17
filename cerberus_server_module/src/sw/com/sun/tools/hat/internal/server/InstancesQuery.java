/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package sw.com.sun.tools.hat.internal.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import sw.cerberus.gson.hprof.AllInstance;
import sw.cerberus.gson.hprof.Classes;
import sw.cerberus.gson.hprof.MainObject;
import sw.cerberus.gson.hprof.RefObject;
import sw.cerberus.gson.hprof.Writer;
import sw.com.sun.tools.hat.internal.model.JavaClass;
import sw.com.sun.tools.hat.internal.model.JavaHeapObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * @author      Bill Foote
 */


class InstancesQuery extends QueryHandler {

    private boolean includeSubclasses;
    private boolean newObjects;

	private AllInstance inst = new AllInstance();
	private AllInstance inst_for_list = new AllInstance();
	private Classes classes = new Classes();

	
	
    public InstancesQuery(boolean includeSubclasses) {
        this.includeSubclasses = includeSubclasses;
    }

    public InstancesQuery(boolean includeSubclasses, boolean newObjects) {
        this.includeSubclasses = includeSubclasses;
        this.newObjects = newObjects;
    }

    public MainObject run(MainObject mainobject) {
        JavaClass clazz = snapshot.findClass(query);
        
		QueryHandler handler = null;
        
        String instancesOf;
        if (newObjects)
            instancesOf = "New instances of ";
        else
            instancesOf = "Instances of ";
        if (includeSubclasses) {
            startHtml(instancesOf + query + " (including subclasses)");
        } else {
            startHtml(instancesOf + query);
        }
        if (clazz == null) {
            error("Class not found");
        } else {
            out.print("<strong>");
            printClass(clazz);
            out.print("</strong><br><br>");
            Enumeration objects = clazz.getInstances(includeSubclasses);
            long totalSize = 0;
            long instances = 0;
            
            classes.init(clazz.getName());
            while (objects.hasMoreElements()) {
                JavaHeapObject obj = (JavaHeapObject) objects.nextElement();
                if (newObjects && !obj.isNew())
                    continue;
                
                
//                handler = new ObjectQuery();
//				handler.setUrlStart("../");
//				handler.setQuery(((JavaHeapObject) obj).getIdString());
//				handler.setOutput(out);
//				handler.setSnapshot(snapshot);
//				handler.run(new MainObject());
				
				
                inst_for_list.setinstance(((JavaHeapObject) obj).getIdString(), obj.getClazz().getName(), obj.getSize());
                instance_list.add(inst_for_list);
                inst_for_list = new AllInstance();             
                
                
                classes.addInstance(inst, ((JavaHeapObject) obj).getIdString(), obj.getClazz().getName() ,obj.getSize());
               
                
                out.println("<br>");
                totalSize += obj.getSize();
                instances++;
            }	
            mainobject.setClass(classes);            
            
            
            
            out.println("<h2>Total of " + instances + " instances occupying " + totalSize + " bytes.</h2>");
        }		
        endHtml();
        return mainobject;
    }
    
    
}
