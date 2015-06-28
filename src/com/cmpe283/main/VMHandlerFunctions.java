package com.cmpe283.main;

import java.net.URL;
import java.rmi.RemoteException;

import com.vmware.vim25.AlarmState;
import com.vmware.vim25.ComputeResourceConfigSpec;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostVMotionCompatibility;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.Permission;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VMHandlerFunctions
{
    
    public static ManagedEntity[] getAllVM(ServiceInstance si)
    {
        ManagedEntity[] mes = null;
        try
        {
            mes = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("VirtualMachine");
            System.out.println("RootFolder: "+si.getRootFolder()+" count of VM: "+mes.length);
        }
        catch (Exception e)
        {
            System.out.println("** ERROR in getAllVm ** "+e.getMessage());
            e.printStackTrace();
        }
        return mes;
    }
    
    public static ServiceInstance CreateServiceInstance(URL url, String username, String password) throws Exception
    {
        ServiceInstance si = new ServiceInstance(url, username, password, true);
        return si;
    }
    
    public static boolean pingIP(String ip) throws Exception {
        String cmd = "";
        
        if(ip != null)
        {
        
            if (System.getProperty("os.name").startsWith("Windows")) {
                // For Windows
                cmd = "ping -n 3 " + ip;
            } else {
                // For Linux and OSX
                cmd = "ping -c 3 " + ip;
            }
        
            System.out.println("Ping "+ ip + "......");
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();      
            return process.exitValue() == 0;
        }
        else
        {
            System.out.println("Wating .... ");
            Thread.sleep(1000);
            return false;
        }
    }
    
    public static boolean checkPowerOffAlarm(Alarm alarm, VirtualMachine vm) {
        AlarmState[] as = vm.getTriggeredAlarmState();
        if (as == null)
            return false;
        for (AlarmState state : as) {
            // if the vm has a poweroff alarm, return true;
            if (alarm.getMOR().getVal().equals(state.getAlarm().getVal()))
                return true;
        }
        return false;
    }
    
    public static HostSystem findHost(VirtualMachine vm) throws InvalidProperty, RuntimeFault, RemoteException{
        HostSystem vmHost = null;
            ManagedEntity[] hosts = new InventoryNavigator(AvailabilityManager.si.getRootFolder()).searchManagedEntities("HostSystem");
            for(int i=0; i<hosts.length; i++){
                //System.out.println("host["+i+"]=" + hosts[i].getName());
                HostSystem h = (HostSystem) hosts[i];
                VirtualMachine vms[] = h.getVms();
                for (int p = 0; p < vms.length; p++) {
                    VirtualMachine v = (VirtualMachine) vms[p];
                    if ((v.getName().toLowerCase()).equals(vm.getName().toLowerCase())) {
                        vmHost = (HostSystem) hosts[i];
                        break;
                    }
                }
            }
        return vmHost;
    }
    
    public static void VMRevert(VirtualMachine revertvm) throws Exception
    {
            Task task = revertvm.revertToCurrentSnapshot_Task(null);
            if(task.waitForTask()==Task.SUCCESS)
            {
                System.out.println("Reverted to snapshot:" + revertvm.getName());
            }
            VMHandlerFunctions.powerOn(revertvm);
    }
    
    public static void HostRevert(HostSystem revertHost) throws Exception
    {
        //Create admin service instance
        ServiceInstance si = CreateServiceInstance(AvailabilityManager.adminVCurl, AvailabilityManager.username_19, AvailabilityManager.password);
        
        String hostName = revertHost.getName().substring(7);
        hostName = "T14-vHost0"+revertHost.getName().substring(12)+"_"+hostName;
        
        //Search failed host name 
        ManagedEntity[] mes = VMHandlerFunctions.getAllVM(si);
        for(int i=0; i < mes.length; i++)
        {
            VirtualMachine vm = (VirtualMachine)mes[i];
            if(vm != null)
            {
                System.out.println("vm name= "+vm.getName() +" ... "+hostName);
                if (vm.getName().equals(hostName))
                {
                    //Revert
                    System.out.println("Reverting host:" + vm.getName());
                    Task task = vm.revertToCurrentSnapshot_Task(null);
                    if(task.waitForTask()==Task.SUCCESS)
                    {
                        VMHandlerFunctions.powerOn(vm);
                        System.out.println("Reverted to snapshot:" + vm.getName());
                    }
                }
            }
        }
    }
    
    public static void powerOn(VirtualMachine vm) throws Exception
    {
        VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();
        Task task = vm.powerOnVM_Task(null);
        if(vmri.getPowerState() == VirtualMachinePowerState.poweredOff)
        {
            if(task.waitForTask() == Task.SUCCESS)
                System.out.println("vm:" + vm.getName() + " powered on.");
            else
                System.out.println("Can not power on " + vm.getName());
        }
        else
            System.out.println("vm:" + vm.getName() + " is running.");
    }
    
    public static HostSystem checkOtherLiveHost(HostSystem oldHost) throws Exception
    {
        Folder rootFolder = AvailabilityManager.si.getRootFolder();
        ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
        for(int i=0; i<hosts.length; i++)
        {
            HostSystem h = (HostSystem) hosts[i];
            if(!h.getName().equals(oldHost.getName()))
            {
                if(VMHandlerFunctions.pingIP(h.getName()))
                {
                    return h;
                }
            }
        }
        return null;
    }
    
    public static void migrateVM(HostSystem oldHost, VirtualMachine vm) throws Exception
    {
        
        ServiceInstance si = AvailabilityManager.si;
        Folder rootFolder = AvailabilityManager.si.getRootFolder();
        HostSystem newHost = VMHandlerFunctions.checkOtherLiveHost(oldHost);
        if (newHost == null)
        {
        	HostConnectSpec hcSpec = new HostConnectSpec();
			hcSpec.setHostName("130.65.133.21");
			hcSpec.setUserName("root");
			hcSpec.setPassword("12!@qwQW");
			hcSpec.setSslThumbprint("59:C8:26:6F:FE:36:8D:A0:AF:B5:62:C6:75:03:68:D4:14:99:3D:C5");

			ComputeResourceConfigSpec compResSpec = new ComputeResourceConfigSpec();
			
			Task task  = null;

			try {

			Permission permission = new Permission();
			permission.setPropagate(true);

			permission.setEntity(AvailabilityManager.si.getMOR());

			ManagedEntity[] dcs = new InventoryNavigator(AvailabilityManager.si.getRootFolder()).searchManagedEntities("Datacenter");


			task = ((Datacenter)dcs[0]).getHostFolder().addStandaloneHost_Task(hcSpec, compResSpec, true);

			try {

			if(task.waitForTask() == Task.SUCCESS){

			System.out.println("Host Created Succesfully");
	        ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
	        for(int i=0; i<hosts.length; i++)
	        {
	            HostSystem h = (HostSystem) hosts[i];
	            if(h.getName().equals("130.65.133.21"))
	            {
	            	newHost = h;
	            }
	        }
			}

			} catch (Exception e) {

			System.out.println("Error in creating a new vHost2 : " + e);
			}

			} catch (Exception e) {

			System.out.println("Error in creating a new vHost : " + e);

			}
        }
        
        if(VirtualMachinePowerState.poweredOff == vm.getSummary().getRuntime().powerState)
        {
            ComputeResource cr = (ComputeResource) newHost.getParent();
            
            String[] checks = new String[] {"cpu", "software"};
            HostVMotionCompatibility[] vmcs =
              si.queryVMotionCompatibility(vm, new HostSystem[] 
                 {newHost},checks );
            
            String[] comps = vmcs[0].getCompatibility();
            if(checks.length != comps.length)
            {
              System.out.println("CPU/software NOT compatible. Exit.");
              return;
            }
            
            Task task = vm.migrateVM_Task(cr.getResourcePool(), newHost,
                VirtualMachineMovePriority.highPriority, 
                VirtualMachinePowerState.poweredOff);
          
            if(task.waitForMe()==Task.SUCCESS)
            {
              System.out.println("***************Migrated******************");
              VMHandlerFunctions.powerOn(vm);
              
            }
            else
            {
              System.out.println("VMotion failed!");
              TaskInfo info = task.getTaskInfo();
              System.out.println(info.getError().getFault());
            }
        }
        else
        {
            System.out.println("Reverting to " + vm.getSnapshot() + "...");

            ComputeResource cr = (ComputeResource) newHost.getParent();
            String vmName = vm.getName();
            //Folder rootFolder = ServiceInstanceSingleton.getServiceInstance().getRootFolder();
            Datacenter dc;
            try
            {

                dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", "T14-DC");

                String vmxPath = "[nfs4team14]" + vmName + "/" + vmName + ".vmx";
                VirtualMachine oldHostVM = null;
                ResourcePool rp = cr.getResourcePool();
                ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
                Task destroyHost = oldHost.getParent().destroy_Task();
                if(destroyHost.waitForTask() == Task.SUCCESS)
                {
                Task registerVM = dc.getVmFolder().registerVM_Task(vmxPath,

                vmName, false, rp, newHost);

                if (registerVM.waitForTask() == Task.SUCCESS)
                {

                    System.out.println("Registered Success");
                    VMHandlerFunctions.powerOn(vm);

                }
                }
                /*************/

            }
            catch (InvalidProperty e)
            {
                e.printStackTrace();

            }
            catch (RuntimeFault e)
            {
                e.printStackTrace();

            }
            catch (RemoteException e)
            {

                e.printStackTrace();

            }
            catch (InterruptedException e)
            {
                e.printStackTrace();

            }

        }
    }
}
