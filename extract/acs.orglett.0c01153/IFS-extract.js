{"IFS-extract-version":"0.1.0-alpha","pathway":[
 {"hash":"0c01153"},
 {"pubid":"acs.orglett.{hash}"},
 {"src":"IFS.finding.aid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
 {"data1":"IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_002.zip"},
 {"data2":"IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_003.zip"},

 {"objects":"IFS.finding.aid.object::{data1}|{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}/{IFS.nmr.param.expt::*}/**/*}"},
 {"objects":"IFS.finding.aid.object::{data1}|{IFS.structure.representation.cdx::{id=IFS.structure.param.compound.id::*}/*.cdx}"},
 {"objects":"IFS.finding.aid.object::{data2}|{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}/{IFS.nmr.param.expt::*}/**/*}"},
 {"objects":"IFS.finding.aid.object::{data2}|{IFS.structure.representation.cdx::{id=IFS.structure.param.compound.id::*}/*.cdx}"},
}