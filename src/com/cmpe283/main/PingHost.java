package com.cmpe283.main;

import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.VirtualMachine;

public class PingHost implements Runnable {
	
	public VirtualMachine vm;
	public HostSystem host;
	public String hostIP;
	public boolean flag;
	
	public PingHost(VirtualMachine vm)
	{
		this.vm = vm;
	}
	
	@Override
	public void run()
	{
		//Find HOst from VMFunctions
		try {
			host = VMHandlerFunctions.findHost(vm);
			//System.out.println(host);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		hostIP = host.getName();
		System.out.println("HOST IP: " + hostIP);
		try {
			if (VMHandlerFunctions.pingIP(hostIP))
			{
				System.out.println("HOST " + host.getName() + "  is RUNNING...." );
				//recoverVM
				VMHandlerFunctions.VMRevert(vm);
			}
			else
			{
				System.out.println("HOST " + host.getName() + "  is NOT RUNNING...." );
				//recoverHost
				try
				{
				    VMHandlerFunctions.HostRevert(host);
				}
				catch (Exception e) 
				{
					System.out.println("******Host is down******");
				   e.printStackTrace();
				    flag=true;
				    VMHandlerFunctions.migrateVM(host, vm);
                }
				
				if(!flag)
				{
				  //revert VM on host
	                VMHandlerFunctions.VMRevert(vm);
	                flag = false;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Waiting...");
		}
		
	}
}
