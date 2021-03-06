package org.jgroups.tools.analyser.handlers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.ISourceProviderService;
import org.jgroups.tools.analyser.service.IPcapService;
import org.jgroups.tools.analyser.service.PacketSniffer;
import org.jgroups.tools.analyser.service.SourceProvider;
import org.jgroups.tools.analyser.views.PacketTableView;
import org.jnetpcap.packet.PcapPacket;


public class LoadHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		final PacketTableView pv = (PacketTableView)HandlerUtil.getActivePart(event).getSite().getPage().findView("JGroupsAnalyser.PacketTableView");

		FileDialog dialog = new FileDialog(window.getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.pcap*", "*.*"});
		dialog.setFilterNames(new String[] {"Pcap Files", "All Files"});
		String fileSelected = dialog.open();
		if(fileSelected == null) {
			return null;
		}


		IPcapService p = (IPcapService) PlatformUI.getWorkbench().getService(IPcapService.class);
		ISourceProviderService sourceProviderService = (ISourceProviderService) HandlerUtil.getActiveWorkbenchWindow(event).getService(ISourceProviderService.class);
		SourceProvider commandStateService = (SourceProvider) sourceProviderService.getSourceProvider(SourceProvider.PLAY_STATE);
		commandStateService.toogleLoad();
		commandStateService.tooglePlay();
		PacketSniffer packetSniffer = new PacketSniffer(dialog.getFilterPath() + File.separator + dialog.getFileName());
		LinkedBlockingQueue<PcapPacket> q = new LinkedBlockingQueue<PcapPacket>();
		MyPcapPacketHandler handler = new MyPcapPacketHandler(q);

		ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event).getShell());
		
		
		ProcessPacketHandler pph =  new ProcessPacketHandler(pv, q, commandStateService);
		packetSniffer.setPacketHandler(handler);

		p.setPcapHandler(packetSniffer);
		p.setProcessPacketHandler(pph);
		p.setQueue(q);

		packetSniffer.start();
		try {
			packetSniffer.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("after join, q size : " + q.size());
		try {
			monitorDialog.run(true, true, pph);
//			window.run(true, true, pph);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}
	
	@Override
	public boolean isHandled() {
		return true;
	}

}
