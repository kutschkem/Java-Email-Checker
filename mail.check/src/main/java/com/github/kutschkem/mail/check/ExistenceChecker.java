package com.github.kutschkem.mail.check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.google.common.base.Splitter;

/**
 * Port of the method described at 
 * <a href="http://www.webdigi.co.uk/blog/2009/how-to-check-if-an-email-address-exists-without-sending-an-email/">
 * this site</a> to check the validity of an address. Current status:
 * 
 * gmail - doesn't seem to work, always reports as correct
 * yahoo - doesn't work because dial-up ip's are blacklisted, need a proxy?
 * hotmail - doesn't seem to work either, only get error at connection startup, probably blacklisted as well
 * @author Michael Kutschke
 *
 */

public class ExistenceChecker {
	
	private String sourceHost;
	private String sourceId;

	public ExistenceChecker(String sourceHost, String sourceId) {
		this.sourceHost = sourceHost;
		this.sourceId = sourceId;
		
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		
		boolean exists = new ExistenceChecker(args[1], args[2]).exists(args[0]);
		System.out.println(exists);

	}
	
	public boolean exists(String address) throws UnknownHostException, IOException{
		Splitter splitter = Splitter.on('@');
		List<String> splitAddress = splitter.splitToList(address);
		if(splitAddress.size() != 2){
			throw new IllegalArgumentException();
		}
		
		Record[] mxRecords = lookupMxRecords(splitAddress.get(1));
		
		boolean found = false;
		for(Record record : mxRecords){
			try{
			found |= queryForAddress(((MXRecord)record).getTarget(),address);
			if(found){
				break;
			}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		return found;
		
	}
	
	private boolean queryForAddress(Name target, String address) throws UnknownHostException, IOException {
		Socket socket = new Socket(target.toString(),25);
		PrintStream outputStream = new PrintStream(socket.getOutputStream());
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		String line = inputStream.readLine();
		if(! line.startsWith("220")){
			throw new IOException(line);
		}
		
		outputStream.println("helo " + sourceHost);
		
		line = inputStream.readLine();
		if(! line.startsWith("250")){
			throw new IOException(line);
		}
		
		outputStream.println("mail from: <"+ sourceId + "@" + sourceHost + ">");
		
		line = inputStream.readLine();
		if(! line.startsWith("250")){
			throw new IOException(line);
		}
		
		outputStream.println("rcpt to: <"+address+">");
		
		boolean found;
		
		if(line.startsWith("250") || line.startsWith("451") || line.startsWith("452")){
			System.out.println("DEBUG: " + line);
			found = true;
		}else{
			found = false;
		}
		
		
		outputStream.println("quit");
		
		socket.close();

		return found;
		
	}

	private Record[] lookupMxRecords(final String domainPart) throws TextParseException
	{
	    final Lookup dnsLookup = new Lookup(domainPart, Type.MX);
	    return dnsLookup.run();
	}

}
