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

// menu

+ BigramEditorWindow {

	menuElementAction { |row, column|

		/*bigramOptionsMenuElements = 		[
		["File", "new", "open", "close", "save", "saveAs", "import", "export", "print", "configure"],
		["Edit", "undo", "redo", "add bars", "tempo"],
		["Region", "duplicate", "group", "ungroup", "block", "unblock"],
		["Track", "new","duplicate","delete"]
		];*/

		switch (row)
		{0} { // FILE
			switch (column)
			{1} { //new
				this.removeAllTracks;
				// new editor instance
				bigramEditor = BigramEditor.new(this);
				savePath = nil;
				cursorPulseIndex=nil;
				this.addBars;
			}
			{2} { //open
				// do all done in new

				this.removeAllTracks;
				// new editor instance
				bigramEditor = BigramEditor.new(this);
				savePath = nil;
				cursorPulseIndex=nil;

				this.loadDocument;
			}
			{3} { //close
				this.removeAllTracks;
				savePath = nil;
				cursorPulseIndex=nil;
				this.refresh;
			}
			{4} { //save
				if (savePath.isNil) {
					this.saveDocumentAs;
				} {
					this.saveDocument;
				}
			}
			{5} { //saveAs
				this.saveDocumentAs;
			}
			{6} { //import
				Dialog.openPanel(okFunc:{ |path|
					// create new
					this.removeAllTracks;
					// new editor instance
					bigramEditor = BigramEditor.new(this);
					savePath = nil;
					cursorPulseIndex=nil;
					// load midi
					bigramEditor.importMidiFile(path);
				})
			}
			{7} { //export

			}
			{8} { //print

			}
			{9} { //configure

			}
			{/*other*/
			}
		}
		{1} { // EDIT
			switch (column)
			{1} { //undo

				if (bigramEditor.tmpVersion >= 1 ) {
					var path;

					// remove tracks
					this.removeAllTracks;
					// remove bars
					bigramEditor.tempos= List.new;
					bigramEditor.barsFromPulses = List.new;
					bigramEditor.numBars = 0;
					bigramEditor.measuresFromBars = List.new;
					// remove bar views
					this.recalculateCanvasSize;

					bigramEditor.tmpVersion = bigramEditor.tmpVersion - 1;
					path = bigramEditor.tmpFilePath ++ "_" ++ bigramEditor.tmpVersion.asString;
					this.loadTmp(path);
					["bigramEditor.tmpVersion",bigramEditor.tmpVersion].postln;
					["bigramEditor.tmpMaxVersion",bigramEditor.tmpMaxVersion].postln;

				} {
					"original state".postln;
				}

			}
			{2} { //redo
				var path;

				//load
				if (bigramEditor.tmpVersion < bigramEditor.tmpMaxVersion) {

					// remove tracks
					this.removeAllTracks;
					// remove bars
					bigramEditor.tempos= List.new;
					bigramEditor.barsFromPulses = List.new;
					bigramEditor.numBars = 0;
					bigramEditor.measuresFromBars = List.new;
					// remove bar views
					this.recalculateCanvasSize;

					bigramEditor.tmpVersion = bigramEditor.tmpVersion + 1;
					path = bigramEditor.tmpFilePath ++ "_" ++ bigramEditor.tmpVersion.asString;
					this.loadTmp(path);
					["bigramEditor.tmpVersion",bigramEditor.tmpVersion].postln;
					["bigramEditor.tmpMaxVersion",bigramEditor.tmpMaxVersion].postln;
				} {
					"last version".postln;
				};
			}
			{3} { // add bars
				this.addBars;
			}

			{4} { // set tempo
				this.setTempo;
			}

		}
		{2} { // REGION
			switch (column)
			{1} { // duplicate
				bigramEditor.bigramTracks.do{ |track|
					track.bigramRegions.do{ |region,regionIndex|
						if (region.selected) {
							var regionName = track.bigramRegionNames.at(regionIndex);
							track.duplicateRegion(regionName);
						}
					}
				};
				this.updateView;

			}
			{2} { // group
				bigramEditor.bigramTracks.do{ |track|
					var regions = track.bigramRegions.select(_.selected);
					if (regions.size > 1) {
						track.groupRegions(regions[0],regions[1])
					}
				};
				this.updateView;
			}
			{3} { // split
				var cursorBPD = bigramEditor.getBPDfromPulseIndex(cursorPulseIndex);
				bigramEditor.bigramTracks.do{ |track|
					track.bigramRegions.do{ |region,regionIndex|
						if (region.selected) {
							var regionName = track.bigramRegionNames.at(regionIndex);
							track.splitRegion(regionName,cursorBPD);
							// track.resizeRegion(regionName,\end,cursorBPD);
						}
					}
				};
				this.updateView;

			}
			{4} { // block

			}
			{5} { // unblock

			}
			/*{6} { //delete

			}
			{7} { //group

			}
			{8} { //ungroup

			}
			{9} { //block

			}
			{10} { //unblock

			}
			{11} { //preferencies

			}*/
			{/*other*/}
			//undo, redo, cut, copy, paste, delete, group/ungroup, block/unblock, preferencies
		}
		{3} { // TRACK
			switch (column)
			{1} { //new
				"add track".postln;
				this.addTrack(save:true);
			}
			{2} { //duplicate
				var trackName = bigramEditor.bigramTrackNames.at(selectedTrackIndex);
				this.duplicateTrack(trackName);
				"save duplicate track".postln;
				this.saveTmp;
			}
			{3} { //delete
				var a = selectedTrackIndex.postln;
				var trackName = bigramEditor.bigramTrackNames.at(selectedTrackIndex);
				this.removeTrack(trackName);
				"save remove Track".postln;
				this.saveTmp;
			}
			{/*other*/}
		}
	}

}
