package test;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import com.vmware.vim25.HostVMotionCompatibility;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class MigrateVM
{

    /**
     * @param args
     * @throws MalformedURLException 
     * @throws RemoteException 
     */
    public static void main(String[] args) throws RemoteException, MalformedURLException
    {
        String vmname = "TM14-VM03-Ubu";
        String newHostName = "130.65.133.23";

        ServiceInstance si = new ServiceInstance(
            new URL("https://130.65.132.114/sdk"), "administrator", "12!@qwQW", true);

        Folder rootFolder = si.getRootFolder();
        VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
            rootFolder).searchManagedEntity(
                "VirtualMachine", vmname);
        HostSystem newHost = (HostSystem) new InventoryNavigator(
            rootFolder).searchManagedEntity(
                "HostSystem", newHostName);
        ComputeResource cr = (ComputeResource) newHost.getParent();
        
        String[] checks = new String[] {"cpu", "software"};
        HostVMotionCompatibility[] vmcs =
          si.queryVMotionCompatibility(vm, new HostSystem[] 
             {newHost},checks );
        
        String[] comps = vmcs[0].getCompatibility();
        if(checks.length != comps.length)
        {
          System.out.println("CPU/software NOT compatible. Exit.");
          si.getServerConnection().logout();
          return;
        }
        
        Task task = vm.migrateVM_Task(cr.getResourcePool(), newHost,
            VirtualMachineMovePriority.highPriority, 
            VirtualMachinePowerState.poweredOff);
      
        if(task.waitForMe()==Task.SUCCESS)
        {
          System.out.println("VMotioned!");
        }
        else
        {
          System.out.println("VMotion failed!");
          TaskInfo info = task.getTaskInfo();
          System.out.println(info.getError().getFault());
        }
        si.getServerConnection().logout();
    }

}
