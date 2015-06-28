package test;

import com.cmpe283.beans.MyConfig;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VmManager
{

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        CloneVm();
    }
    
    private static void CloneVm() throws Exception {
        ServiceInstance si = new ServiceInstance(MyConfig.getVCenterURL(), MyConfig.getUsername(), MyConfig.getPassword(), true);
        Folder rootFolder = si.getRootFolder();

        String TEAM14_DC="Team14_DC";
        Datacenter dc = (Datacenter) si.getSearchIndex().findByInventoryPath(TEAM14_DC);
        System.out.println("vm folder "+dc.getVmFolder());
        String vmPath = ""+dc.getVmFolder();
        
        /*ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        if(mes==null || mes.length ==0)
        {
            return;
        }
        
        for(int i=0; i<mes.length; i++)
        {
            VirtualMachine vm = (VirtualMachine) mes[i];
            System.out.println("Name: "+vm.getName());
        }*/
        
        VirtualMachine vm = (VirtualMachine) si.getSearchIndex().findByInventoryPath(vmPath);
        System.out.println("name of vm "+vm.getName());
        String hostsystemPath = ""+dc.getHostFolder();
        //HostSystem hs = (HostSystem) si.getSearchIndex().findByInventoryPath(hostsystemPath);
        /*HostSystem targethost = (HostSystem) new InventoryNavigator(
                rootFolder).searchManagedEntity("HostSystem","130.65.33.21" );

        ResourcePool RP = (ResourcePool) new InventoryNavigator(
                rootFolder).searchManagedEntity("ResourcePool","Resources" );


        VirtualMachineRelocateSpec VMrelocateSpec = new VirtualMachineRelocateSpec();
        VMrelocateSpec.setDatastore(dc.getMOR());
        VMrelocateSpec.setHost(targethost.getMOR());
        VMrelocateSpec.setPool(RP.getMOR());


        //  ManagedObjectReference snapshot= vm.getCurrentSnapShot();
        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        cloneSpec.setLocation(VMrelocateSpec);
        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);
        cloneSpec.setSnapshot(vm.getCurrentSnapShot().getMOR());



        Folder vmFolder = dc.getVmFolder();
        String cloneName = "clone"+vm.getName();
        Task task = vm.cloneVM_Task(vmFolder, cloneName, cloneSpec);
        System.out.println("Launching the VM clone task. It might take a while. Please wait for the result ...");

        String status =     task.waitForMe();
        if(status==Task.SUCCESS)
        {
            System.out.println("Virtual Machine got cloned successfully.");
        }
        else
        {
            System.out.println("Failure -: Virtual Machine cannot be cloned");
        }*/
    }

}
