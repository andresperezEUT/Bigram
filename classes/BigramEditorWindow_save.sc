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

// save file in a xml stucture

// TODO: file extensions

+ BigramEditorWindow {

	saveAction { |path|
		var file;
		var d, bigram, tracks, trackList, track;
		var regions, regionList;
		var notes, noteList;
		var bars, bar;

		// create document
		d = DOMDocument.new;

		// create root element "bigram"
		bigram = d.createElement("bigram");
		d.appendChild(bigram);

		// define bars
		bars = d.createElement("bars");
		bigram.appendChild(bars);



		bigramEditor.measuresFromBars.do { |measure,barIndex|
			var bar = d.createElement("bar");

			"-------------".postln;

			barIndex.postln;
			bigramEditor.barsFromPulses.postln;
			bigramEditor.barsFromPulses[0].postln;
			bigramEditor.barsFromPulses[0].class.postln;
			bigramEditor.barsFromPulses.indexOf(0).postln;
			// bigramEditor.barsFromPulses.indexOf(barIndex.asFloat).postln;
			bigramEditor.barsFromPulses.indexOf(barIndex).postln;
			"-------------".postln;

			bar.setAttribute( "index", barIndex.asString );
			bar.setAttribute( "pulses", measure.pulse.asString );
			bar.setAttribute( "divisions", measure.division.asString);
			// bar.setAttribute( "tempo", bigramEditor.tempos[bigramEditor.barsFromPulses.indexOf(barIndex.asFloat)].asString );
			bar.setAttribute( "tempo", bigramEditor.tempos[bigramEditor.barsFromPulses.indexOf(barIndex)].asString );

			bars.appendChild(bar);
		};


		// define tracks
		tracks = d.createElement("tracks");
		bigram.appendChild(tracks);

		// add regions to the tracks
		trackList = bigramEditor.bigramTracks;

		trackList.do { |aTrack, trackIndex|
			var track = d.createElement("track");
			track.setAttribute( "name" , ( aTrack.name.asString ) );
			track.setAttribute( "midiProgram" , ( aTrack.midiProgram.asString ) );
			tracks.appendChild(track);

			regions = d.createElement("regions");
			track.appendChild(regions);

			regionList = aTrack.bigramRegions;

			regionList.do { |aRegion, regionIndex|
				var region = d.createElement("region");
				region.setAttribute( "name" , regionIndex.asString );
				region.setAttribute("sB",aRegion.startBPD.bar.asString);
				region.setAttribute("sP",aRegion.startBPD.pulse.asString);
				region.setAttribute("sD",aRegion.startBPD.division.asString);
				region.setAttribute("eB",aRegion.endBPD.bar.asString);
				region.setAttribute("eP",aRegion.endBPD.pulse.asString);
				region.setAttribute("eD",aRegion.endBPD.division.asString);
				regions.appendChild(region);

				notes = d.createElement("notes");
				region.appendChild(notes);

				noteList = aRegion.notes;

				noteList.do { |aNote, noteIndex|
					var note = d.createElement("note");
					note.setAttribute("height",aNote.pitch.height.asString);
					note.setAttribute("pClass",aNote.pitch.pClass.asString);
					note.setAttribute("b",aNote.bpd.bar.asString);
					note.setAttribute("p",aNote.bpd.pulse.asString);
					note.setAttribute("d",aNote.bpd.division.asString);
					notes.appendChild(note);
				}
			}
		};

		file = File(path, "w");
		d.write(file);
		file.close;
	}

	saveTmp {
		var name;
		if (bigramEditor.tmpVersion.isNil) {
			bigramEditor.tmpVersion = 0;
			bigramEditor.tmpMaxVersion = 0;
		} {
			bigramEditor.tmpVersion = bigramEditor.tmpVersion + 1;
			bigramEditor.tmpMaxVersion = bigramEditor.tmpVersion;
		};
		name = bigramEditor.tmpFilePath ++ "_" ++ bigramEditor.tmpVersion.asString;
		this.saveAction(name);
		["bigramEditor.tmpMaxVersion",bigramEditor.tmpMaxVersion].postln;
	}


	saveDocument {
		this.saveAction(savePath);
	}


	saveDocumentAs {
		Dialog.savePanel(okFunc:{ |path|
			this.saveAction(path);
			savePath = path;
			this.setWindowName;
		})
	}
}
