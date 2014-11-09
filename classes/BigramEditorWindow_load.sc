////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) Andrés Pérez López, November 2014 [contact@andresperezlopez.com]
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; withot even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <<http://www.gnu.org/licenses/>
//
////////////////////////////////////////////////////////////////////////////

// load from a xml file

+ BigramEditorWindow {

/*	loadDocument {

		var filePath, file;
		var document, xmlContent;

		Dialog.openPanel(okFunc:{ |path|

			var track, tracks;
			var region, regions;
			var note, notes;
			var bars, bar;

			// load file

			filePath = path;

			file = File(filePath,"r");
			document = DOMDocument.new;

			xmlContent = String.readNew(file);
			document.parseXML(xmlContent); // parses from string
			file.close;

			// remove all existing tracks
			// TODO: ask for saving

			this.removeAllTracks;
			savePath = path;

			// load bars data
			bars = document.getDocumentElement.getElement("bars");
			bar = bars.getFirstChild;
			while ( { bar != nil }, {
				var barIndex = bar.getAttribute("index").asInt;
				var pulses = bar.getAttribute("pulses").asInt;
				var divisions = bar.getAttribute("divisions").asInt;
				var tempo = bar.getAttribute("tempo").asInt;
				bigramEditor.addBars(1,PD.new(pulses,divisions),tempo);
				// [barIndex,pulses,divisions,tempo].postln;
				bar = bar.getNextSibling;
			} );

			// parse and load into the bigram structure

			tracks = document.getDocumentElement.getElement("tracks");
			track = tracks.getFirstChild;
			while ( { track != nil } , {
				var trackName = track.getAttribute("name");
				// ("Track : " ++ trackName).postln;

				var trackRef = this.addTrack;

				var trackIndex = bigramEditor.bigramTrackNames.indexOf(trackRef);
				var trackMidiProgram = track.getAttribute("midiProgram");
				["trackMidiProgram",trackMidiProgram].postln;
				this.setMidiProgram(trackRef,trackMidiProgram.asInt);

				// regions
				regions = track.getFirstChild;
				region = regions.getFirstChild;
				while ( { region != nil } , {
					var regionName = region.getAttribute("name").asSymbol;
					var sB = region.getAttribute("sB");
					var sP = region.getAttribute("sP");
					var sD = region.getAttribute("sD");
					var eB = region.getAttribute("eB");
					var eP = region.getAttribute("eP");
					var eD = region.getAttribute("eD");

					var startBPD = BPD(sB.asInt,sP.asInt,sD.asInt);
					var endBPD = BPD(eB.asInt,eP.asInt,eD.asInt);

					bigramEditor.bigramTracks.at(trackIndex).addRegion(startBPD,endBPD);

					("Region : " ++ regionName).postln;
					("startBPD : " ++sB++sP++sD).postln;
					("endBPD : " ++eB++eP++eD).postln;

					notes = region.getFirstChild;
					note = notes.getFirstChild;
					while ( { note != nil } , {
						var b = note.getAttribute("b");
						var p = note.getAttribute("p");
						var d = note.getAttribute("d");
						var height = note.getAttribute("height");
						var pClass = note.getAttribute("pClass");

						var bpd = BPD.new(b.asInt,p.asInt,d.asInt);
						var pitch = BPitch.new(height.asInt,pClass.asInt);

						var noteRef = BigramNote.new(pitch,bpd);
						// ("Note :" ++ b++p++d++","++height++pClass).postln;

						var regionIndex = 	bigramEditor.bigramTracks.at(trackIndex).bigramRegionNames.indexOf(regionName);

						bigramEditor.bigramTracks.at(trackIndex).bigramRegions[regionIndex].putNote(noteRef);

						note = note.getNextSibling;
					});


					//next region
					region = region.getNextSibling;
				});


				//next track
				track = track.getNextSibling;
			});


			this.setWindowName;
		});

	}*/

	loadTmp { |tmpPath|
		this.loadAction(tmpPath,save:false);
		// point the windows to their correspondent tracks and regions
		bigramRegionWindowList.do(_.reload);
	}

	loadDocument {
		Dialog.openPanel(okFunc:{ |path|
			this.loadAction(path,save:false);
			this.setWindowName;

			this.saveTmp;
		})
	}

	loadAction { |path,save=true|
		var filePath, file;
		var document, xmlContent;

		var track, tracks;
		var region, regions;
		var note, notes;
		var bars, bar;

		// load file

		filePath = path;

		file = File(filePath,"r");
		document = DOMDocument.new;

		xmlContent = String.readNew(file);
		document.parseXML(xmlContent); // parses from string
		file.close;

		// remove all existing tracks
		// TODO: ask for saving

		this.removeAllTracks;
		savePath = path;

		// load bars data
		bars = document.getDocumentElement.getElement("bars");
		bar = bars.getFirstChild;
		while ( { bar != nil }, {
			var barIndex = bar.getAttribute("index").asInt;
			var pulses = bar.getAttribute("pulses").asInt;
			var divisions = bar.getAttribute("divisions").asInt;
			var tempo = bar.getAttribute("tempo").asInt;
			bigramEditor.addBars(1,PD.new(pulses,divisions),tempo,save:save); // <---------
			// [barIndex,pulses,divisions,tempo].postln;
			bar = bar.getNextSibling;
		} );

		// parse and load into the bigram structure

		tracks = document.getDocumentElement.getElement("tracks");
		track = tracks.getFirstChild;
		while ( { track != nil } , {
			var trackName = track.getAttribute("name");
			// ("Track : " ++ trackName).postln;

			var trackRef = this.addTrack(save:save); // <-----------------

			var trackIndex = bigramEditor.bigramTrackNames.indexOf(trackRef);
			var trackMidiProgram = track.getAttribute("midiProgram");
			["trackMidiProgram",trackMidiProgram].postln;
			this.setMidiProgram(trackRef,trackMidiProgram.asInt,save:save); // <----------

			// regions
			regions = track.getFirstChild;
			region = regions.getFirstChild;
			while ( { region != nil } , {
				var regionName = region.getAttribute("name").asSymbol;
				var sB = region.getAttribute("sB");
				var sP = region.getAttribute("sP");
				var sD = region.getAttribute("sD");
				var eB = region.getAttribute("eB");
				var eP = region.getAttribute("eP");
				var eD = region.getAttribute("eD");

				var startBPD = BPD(sB.asInt,sP.asInt,sD.asInt);
				var endBPD = BPD(eB.asInt,eP.asInt,eD.asInt);

				bigramEditor.bigramTracks.at(trackIndex).addRegion(startBPD,endBPD,save:save); // <------------

				("Region : " ++ regionName).postln;
				("startBPD : " ++sB++sP++sD).postln;
				("endBPD : " ++eB++eP++eD).postln;

				notes = region.getFirstChild;
				note = notes.getFirstChild;
				while ( { note != nil } , {
					var b = note.getAttribute("b");
					var p = note.getAttribute("p");
					var d = note.getAttribute("d");
					var height = note.getAttribute("height");
					var pClass = note.getAttribute("pClass");

					var bpd = BPD.new(b.asInt,p.asInt,d.asInt);
					var pitch = BPitch.new(height.asInt,pClass.asInt);

					var noteRef = BigramNote.new(pitch,bpd);
					// ("Note :" ++ b++p++d++","++height++pClass).postln;

					var regionIndex = 	bigramEditor.bigramTracks.at(trackIndex).bigramRegionNames.indexOf(regionName);

					bigramEditor.bigramTracks.at(trackIndex).bigramRegions[regionIndex].putNote(noteRef,save:save); // <----------

					note = note.getNextSibling;
				});


				//next region
				region = region.getNextSibling;
			});


			//next track
			track = track.getNextSibling;
		});
	}

}
