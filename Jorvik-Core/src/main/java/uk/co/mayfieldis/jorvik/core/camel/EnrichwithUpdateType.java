package uk.co.mayfieldis.jorvik.core.camel;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;


import ca.uhn.fhir.parser.IParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;


public class EnrichwithUpdateType implements AggregationStrategy  {

	public EnrichwithUpdateType(FhirContext ctx)
	{
		this.ctx = ctx;
		
	}
	private  FhirContext ctx;
	
	private static final Logger log = LoggerFactory.getLogger(uk.co.mayfieldis.jorvik.core.camel.EnrichwithUpdateType.class);
	
	private Boolean practitionerCompare(Practitioner oldPractitioner, Practitioner newPractitioner)
	{
		Boolean same = true;
		if (oldPractitioner.getName().get(0).getFamily().size()>0 && newPractitioner.getName().get(0).getFamily().size()>0)
		{
			if (!oldPractitioner.getName().get(0).getFamily().get(0).getValue().equals(newPractitioner.getName().get(0).getFamily().get(0).getValue()))
			{
	//			log.info("#13 Old name"+oldPractitioner.getName().getFamily().get(0).getValue()+" New Name "+newPractitioner.getName().getFamily().get(0).getValue());
				same = false;
			}
		}
		if (oldPractitioner.getName().get(0).getGiven().size()>0 && newPractitioner.getName().get(0).getGiven().size()>0)
		{
			if (!oldPractitioner.getName().get(0).getGiven().get(0).getValue().equals(newPractitioner.getName().get(0).getGiven().get(0).getValue()))
			{
		//		log.info("#14 Old name"+oldPractitioner.getName().getGiven().get(0).getValue()+" New Name "+newPractitioner.getName().getGiven().get(0).getValue());
				same = false;
			}
		}
		// Check organisations - bit more involved to cope with organisations not being in the database
		if (oldPractitioner.getPractitionerRole().get(0).getOrganization().getReference() == null && newPractitioner.getPractitionerRole().get(0).getOrganization().getReference() != null)
		{
			same = false;
		//	log.info("#1 Old Organisation null. New Organisation = "+newPractitioner.getPractitionerRole().get(0).getManagingOrganization().getReference());
		}
		else
		{
			if (oldPractitioner.getPractitionerRole().get(0).getOrganization().getReference() != null && newPractitioner.getPractitionerRole().get(0).getOrganization().getReference() == null)
			{
				same = false;
			//	log.info("#2 Old Organisation = "+oldPractitioner.getPractitionerRole().get(0).getManagingOrganization().getReference()+" New Organisation null. ");
			}
			else
			{
				
				if (oldPractitioner.getPractitionerRole().get(0).getOrganization().getReference() != null && newPractitioner.getPractitionerRole().get(0).getOrganization().getReference() != null)
				{
					
					if (!oldPractitioner.getPractitionerRole().get(0).getOrganization().getReference().equals(newPractitioner.getPractitionerRole().get(0).getOrganization().getReference()))
					{
				//		log.info("#3 Old Organisation = "+oldPractitioner.getPractitionerRole().get(0).getManagingOrganization().getReference()+" New Organisation = "+newPractitioner.getPractitionerRole().get(0).getManagingOrganization().getReference());
						same = false;
					}
				}
			}
		}
		return same;
	}
	
	private Boolean organisationCompare(Organization oldOrganisation, Organization newOrganisation)
	{
		Boolean same = true;
		log.debug("Check 1");
		if (!oldOrganisation.getName().equals(newOrganisation.getName()))
		{
			same = false;
		//	log.info("#4 Name "+oldOrganisation.getName()+" "+newOrganisation.getName());
		}
		log.debug("Check 2");
		if (oldOrganisation.getActive() != newOrganisation.getActive())
		{
			same = false;
		//	log.info("#5 Active "+oldOrganisation.getActive() + " " + newOrganisation.getActive());
		}
		log.debug("Check 3");
		if (oldOrganisation.getTelecom().size() != oldOrganisation.getTelecom().size())
		{
			same =false;
		}
		log.debug("Check 4");
		if (oldOrganisation.getTelecom().size() > 0  && oldOrganisation.getTelecom().size() > 0)
		{
			if (!oldOrganisation.getTelecom().get(0).getValue().equals(newOrganisation.getTelecom().get(0).getValue()))
			{
				same = false;
			//	log.info("#6 Telecom "+oldOrganisation.getTelecom().get(0).getValue()+" "+newOrganisation.getTelecom().get(0).getValue());
			}
		}
		log.debug("Check 5");
		if (oldOrganisation.getAddress().size() != oldOrganisation.getAddress().size())
		{
			same =false;
		}
		log.debug("Check 6");
		if (oldOrganisation.getAddress().size() >0 && oldOrganisation.getAddress().size()>0)
		{	
			if (!oldOrganisation.getAddress().get(0).getLine().get(0).getValue().equals(newOrganisation.getAddress().get(0).getLine().get(0).getValue()))
			{
				same = false;
			//	log.info("#7 Line 1 "+oldOrganisation.getAddress().get(0).getLine().get(0).getValue()+" "+newOrganisation.getAddress().get(0).getLine().get(0).getValue());;
			}
			if (!oldOrganisation.getAddress().get(0).getPostalCode().equals(newOrganisation.getAddress().get(0).getPostalCode()))
			{
				same = false;
			//	log.info("#8 PostCode "+oldOrganisation.getAddress().get(0).getPostalCode()+" "+newOrganisation.getAddress().get(0).getPostalCode());;
			}
		}
		log.debug("Check 7");
		String oldRef="";
		String newRef="";
		if (oldOrganisation.getPartOf() !=null && oldOrganisation.getPartOf().getReference() !=null)
		{
			oldRef=oldOrganisation.getPartOf().getReference();
		}
		if (newOrganisation.getPartOf() !=null && newOrganisation.getPartOf().getReference() !=null)
		{
			newRef=newOrganisation.getPartOf().getReference();
		}
		if (!oldRef.equals(newRef))
		{
			same=false;
		}
		
		return same;
	}
	
	@Override
	public Exchange aggregate(Exchange exchange, Exchange enrichment) 
	{
		exchange.getIn().setHeader(Exchange.HTTP_METHOD,"GET");
		log.debug("Update Resource Start. Previous Resonse "+enrichment.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE).toString());
		if (enrichment.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE).toString().equals("200"))
		{
			
			//ByteArrayInputStream xmlContentBytes = new ByteArrayInputStream ((byte[]) enrichment.getIn().getBody(byte[].class));
			Reader reader = new InputStreamReader(new ByteArrayInputStream ((byte[]) enrichment.getIn().getBody(byte[].class)));
			Bundle bundle = null;
			
			if (enrichment.getIn().getHeader(Exchange.CONTENT_TYPE).toString().contains("json"))
			{
				//JsonParser parser = new JsonParser();
				IParser parser = ctx.newJsonParser();
				try
				{
					bundle = parser.parseResource(Bundle.class, reader);
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
					throw ex;
				}
			}
			else
			{
				// XmlParser parser = new XmlParser();
				IParser parser = ctx.newXmlParser();
				try
				{
					bundle = parser.parseResource(Bundle.class, reader);
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
					throw ex;
				}
			}
			
			if (bundle!=null && bundle.getEntry().size()==0)
			{
				log.debug("Update Resource - no data found");
				// No resource found go ahead
				exchange.getIn().setHeader(Exchange.HTTP_METHOD,"POST");	
				if (exchange.getIn().getHeader("FHIRResource").toString().contains("Organization"))
				{
					exchange.getIn().setHeader("FHIRResource","Organization");
				}
				if (exchange.getIn().getHeader("FHIRResource").toString().contains("Practitioner"))
				{
					exchange.getIn().setHeader("FHIRResource","Practitioner");
				}
				if (exchange.getIn().getHeader("FHIRResource").toString().contains("Location"))
				{
					exchange.getIn().setHeader("FHIRResource","Location");
				}
			}
			
			if (bundle!=null && bundle.getEntry().size()>0)
			{
				log.debug("Update Resource - Resource found="+bundle.getEntry().size());
				// This is bit over complex. It converts incoming data into generic FHIR Resource and the converts them to JSON for comparison
				
				//Resource oldResource=bundle.getEntry().get(0).getResource();
				Organization oldOrganisation = null;
				Practitioner oldPractitioner = null;
				Location oldLocation = null;
				Organization newOrganisation = null;
				Practitioner newPractitioner = null;
				Location newLocation = null;
				if (exchange.getIn().getHeader("FHIRResource").toString().contains("Organization"))
				{
					oldOrganisation = (Organization) bundle.getEntry().get(0).getResource();
				}
				
				if (exchange.getIn().getHeader("FHIRResource").toString().contains("Practitioner"))
				{
					oldPractitioner = (Practitioner) bundle.getEntry().get(0).getResource();
				}
				if (exchange.getIn().getHeader("FHIRResource").toString().contains("Location"))
				{
					oldLocation = (Location) bundle.getEntry().get(0).getResource();
				}
				log.debug("Processed OLD response to resource object");
				//ByteArrayInputStream xmlNewContentBytes = new ByteArrayInputStream ((byte[]) exchange.getIn().getBody(byte[].class));
				Reader readerNew = new InputStreamReader(new ByteArrayInputStream ((byte[]) exchange.getIn().getBody(byte[].class)));
				
				if (exchange.getIn().getHeader(Exchange.CONTENT_TYPE).toString().contains("json"))
				{
					//JsonParser parser = new JsonParser();
					IParser parser = ctx.newJsonParser();
					try
					{
						
						if (exchange.getIn().getHeader("FHIRResource").toString().contains("Practitioner"))
						{
							newPractitioner = parser.parseResource(Practitioner.class, readerNew);
						}
						if (exchange.getIn().getHeader("FHIRResource").toString().contains("Organization"))
						{
							newOrganisation = parser.parseResource(Organization.class, readerNew);
						}
						if (exchange.getIn().getHeader("FHIRResource").toString().contains("Location"))
						{
							newLocation = parser.parseResource(Location.class, readerNew);
						}
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
						throw ex;
					}
				}
				else
				{
					//XmlParser parser = new XmlParser();
					IParser parser = ctx.newXmlParser();
					try
					{
						if (exchange.getIn().getHeader("FHIRResource").toString().contains("Practitioner"))
						{
							newPractitioner = parser.parseResource(Practitioner.class, readerNew);
						}
						if (exchange.getIn().getHeader("FHIRResource").toString().contains("Organization"))
						{
							newOrganisation = parser.parseResource(Organization.class, readerNew);
						}
						if (exchange.getIn().getHeader("FHIRResource").toString().contains("Location"))
						{
							newLocation = parser.parseResource(Location.class, readerNew);
						}
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
						throw ex;
					}
				}
				log.debug("Processed NEW response to resource object");
				Boolean sameResource = false;
				if (oldOrganisation !=null)
				{
					log.debug("Check for differences on the organization resource");
					sameResource = organisationCompare(oldOrganisation, newOrganisation);
				}
				if (oldPractitioner !=null)
				{
					sameResource = practitionerCompare(oldPractitioner,newPractitioner);
				}
				if (oldLocation !=null)
				{
					// this isn't coded for Location
				}
				if (!sameResource)
				{
					// Record is different so update it
					log.debug("Resource is an update");
					
					if (exchange.getIn().getHeader("FHIRResource").toString().contains("Organization"))
					{
						exchange.getIn().setHeader(Exchange.HTTP_METHOD,"PUT");
						exchange.getIn().setHeader("FHIRResource","Organization/"+oldOrganisation.getIdElement().getIdPart());
						log.info("Existing Organization getID = "+oldOrganisation.getId() + " getIdElement().getIdPart()="+oldOrganisation.getIdElement().getIdPart());
						newOrganisation.setId(oldOrganisation.getIdElement().getIdPart());
						String Response = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(newOrganisation);
						//String Response = ResourceSerialiser.serialise(newOrganisation, ParserType.XML);
						exchange.getIn().setBody(Response);
					}
					if (exchange.getIn().getHeader("FHIRResource").toString().contains("Practitioner"))
					{
						exchange.getIn().setHeader(Exchange.HTTP_METHOD,"PUT");
						exchange.getIn().setHeader("FHIRResource","Practitioner/"+oldPractitioner.getIdElement().getIdPart());
						log.info("Existing Practitioner ID = "+oldPractitioner.getIdElement().getIdPart());
						
						newPractitioner.setId(oldPractitioner.getIdElement().getIdPart());
						
						String Response = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(newPractitioner);
						//String Response = ResourceSerialiser.serialise(newPractitioner, ParserType.XML);
						exchange.getIn().setBody(Response);
					}
					if (exchange.getIn().getHeader("FHIRResource").toString().contains("Location"))
					{
						exchange.getIn().setHeader(Exchange.HTTP_METHOD,"PUT");
						exchange.getIn().setHeader("FHIRResource","Location/"+oldLocation.getIdElement().getIdPart());
						log.info("Existing Location ID = "+oldLocation.getIdElement().getIdPart());
						newLocation.setId(oldLocation.getIdElement().getIdPart());
						String Response = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(newLocation);
						//String Response = ResourceSerialiser.serialise(newLocation, ParserType.XML);
						exchange.getIn().setBody(Response);
					}
				}
			}
			log.debug("Update Resource End");
			exchange.getIn().setHeader("FHIRQuery","");
			// XML as Ensemble doesn't like JSON
			exchange.getIn().setHeader(Exchange.CONTENT_TYPE,"application/xml+fhir");
		}
		return exchange;
	}
}


