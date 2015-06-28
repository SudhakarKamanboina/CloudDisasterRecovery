package com.cmpe283.main;

import java.net.URL;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class SnapshotFactory implements Runnable
{
    URL vmURL, hostURL;
    String adminUser, user, pass;
    ServiceInstance vmsi, adminsi;
    
    public SnapshotFactory(URL url, URL adminurl, String username, String username_19, String password)
    {
        vmURL = url;
        hostURL = adminurl;
        user = username;
        adminUser = username_19;
        pass = password;
    }
    
    @Override
    public void run()
    {
        try
        {
            // Create VM Service instance
            vmsi = VMHandlerFunctions.CreateServiceInstance(vmURL, user, pass);

            // Take VM snapshot
            takeVMSnapshot(vmsi);

            // Create Host Service Instance
            adminsi = VMHandlerFunctions.CreateServiceInstance(hostURL, adminUser, pass);

            // Take Host snapshot
            takeHostSnapshot(adminsi);
        }
        catch (Exception e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void takeVMSnapshot(ServiceInstance si)
    {
        ManagedEntity[] mes = VMHandlerFunctions.getAllVM(si);
        for (int i = 0; i < mes.length; i++)
        {
            VirtualMachine vm = (VirtualMachine) mes[i];
            if (vm != null)
                //System.out.println(VmStates.poweredOn.toString().equals(vm.getSummary().getRuntime().powerState.toString()));
                if(VirtualMachinePowerState.poweredOn == vm.getSummary().getRuntime().powerState)
                {
                    Task task = null;
                    String description = "Snapshot_" + vm.getName() + "_" + System.currentTimeMillis();
                    System.out.println("Snapshot of VM: " + vm.getName() + "-" + vm.getGuest().getIpAddress());
                    
                    try
                    {
                        task = vm.createSnapshot_Task(vm.getName(), description, true, true);
                        if (task.waitForMe() == Task.SUCCESS)
                        {
                            System.out.println(vm.getName() + "- Snapshot Created");
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                // TODO: Delete old snapshots after successfully creating new one
            }
     }
    

    public void takeHostSnapshot(ServiceInstance si)
    {
        ManagedEntity[] mes = VMHandlerFunctions.getAllVM(si);
        for (int i = 0; i < mes.length; i++)
        {
            VirtualMachine vm = (VirtualMachine) mes[i];
            if (vm != null)
            {
                System.out.println(vm.getName());
                if (vm.getName().contains("T14-vHost"))
                {
                    // take snapshot of host
                    Task task = null;
                    String description = "Snapshot_" + vm.getName() + "_" + System.currentTimeMillis();
                    System.out.println("Snapshot of VM: " + vm.getName() + "-" + vm.getGuest().getIpAddress());

                    try
                    {
                        task = vm.createSnapshot_Task(vm.getName(), description, true, true);
                        if (task.waitForMe() == Task.SUCCESS)
                        {
                            System.out.println(vm.getName() + "- Snapshot Created");
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    // TODO: Delete old snapshots after successfully creating new one
                }
            }
        }
    }
}
