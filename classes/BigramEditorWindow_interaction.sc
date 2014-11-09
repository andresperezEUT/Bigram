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

// mouse interaction

+ BigramEditorWindow {

	// mouse down

	bigramCanvasMouseDownAction {

		^{ |view, x, y, modifiers, buttonNumber, clickCount|

			var pulseIndex = (x / pulseWidth).floor.asInteger;
			var trackIndex = (y / trackHeight).floor.asInteger;

			var bpd = bigramEditor.getBPDfromPulseIndex(pulseIndex);
			var track = bigramEditor.bigramTracks.at(trackIndex);

			selectedTrackIndex = trackIndex;

			// do things
			if (playButton.value == 0) {

				switch (mode)
				{\pen} {
					if (track.getRegionAtBPD(bpd).isNil) {
						startRegionBPD = bigramEditor.getBPDfromPulseIndex(pulseIndex);
						// deselectAllRegions
						bigramEditor.bigramTracks.do(_.deselectAllRegions);
					} {
						// don't create region since it will overlap
						startRegionBPD = nil;
					}

				}

				{\pointer} {
					// select / deselect region
					var regionName = track.getRegionAtBPD(bpd);
					if (holdSelection.not) {
						bigramEditor.bigramTracks.do(_.deselectAllRegions);
					};
					if (regionName.isNil.not) {  //regionName or nil
						var region = track.bigramRegions.at(track.bigramRegionNames.indexOf(regionName));
						region.selected_(true);

						// double click: open region window
						if (clickCount == 2) {
							this.openBigramWindow(track,trackIndex.asSymbol,region,regionName);
						};

						// pointer in resize area
						//left
						if (bigramEditor.getBPDfromPulseIndex(pulseIndex).equal(region.startBPD)) {
							if ((x/pulseWidth).frac < 0.5 and:{((y/trackHeight).frac > 0.75)})
							{
								resizeRegion = regionName;
								resizeDir = \init;
								resizeTrack = track;
							} {
								resizeRegion = nil;
							};
						};
						//right
						if (bigramEditor.getBPDfromPulseIndex(pulseIndex).equal(region.endBPD)) {
							if ((x/pulseWidth).frac > 0.5 and:{((y/trackHeight).frac > 0.75)})
							{
								resizeRegion = regionName;
								resizeDir = \end;
								resizeTrack = track;
							} {
								if (region.startBPD.equal(region.endBPD).not) {
									resizeRegion = nil;
								}
							};
						};

						// move region
						if (resizeRegion.isNil) {
							moveRegion = regionName;
							moveTrack = track;
							moveIndex = pulseIndex;
						}
					};
					// update cursor position
					cursorPulseIndex = pulseIndex;
				}

				{\rubber} {
					// delete region
					var regionName = track.getRegionAtBPD(bpd).postln;
					if (regionName.isNil.not) {
						track.removeRegion(regionName);
						/*					"save delete region".postln;
						this.saveTmp;*/
					}
				};
				this.updateView;
			}
		}
	}

	// mouse move
	bigramCanvasMouseMoveAction {

		^{ |view, x, y, modifiers|

			var pulseIndex = (x / pulseWidth).floor.asInteger;
			var trackIndex = (y / trackHeight).floor.asInteger;
			var track = bigramEditor.bigramTracks.at(trackIndex);

			if (playButton.value == 0) {
				switch(mode)
				{\pointer} {
					// update cursor position
					cursorPulseIndex = pulseIndex;
					this.updateView;

					//in case, resize region
					if (resizeRegion.isNil.not) {
						var bpd = bigramEditor.getBPDfromPulseIndex(pulseIndex);
						resizeTrack.resizeRegion(resizeRegion,resizeDir,bpd);
					};

					// in case, move region
					if (moveRegion.isNil.not) {
						var dir;
						var numDivisions = moveIndex - pulseIndex;
						if (numDivisions > 0 ) {
							dir = \left;
						} {
							numDivisions = numDivisions.abs;
							dir = \right;
						};
						if (moveTrack == track) {
							if (numDivisions > 0 ) {
								// move along track
								moveTrack.moveRegion(moveRegion,numDivisions,dir);
								moveIndex = pulseIndex;
								// "save move region".postln; moveRegion will save
								// this.saveTmp;
							}
						} {
							// move to another track
							var regionIndex = moveTrack.bigramRegionNames.indexOf(moveRegion);
							var region = moveTrack.bigramRegions.at(regionIndex);
							var newRegionName = track.addRegion(region.startBPD,region.endBPD,save:false);
							var newRegionIndex = track.bigramRegionNames.indexOf(newRegionName);
							var newRegion = track.bigramRegions.at(newRegionIndex);
							region.notes.do{|note| newRegion.putNote(note.copy)};
							// remove old region
							moveTrack.removeRegion(moveRegion,save:false);

							moveTrack = track;
							moveRegion = newRegionName;
							moveIndex = pulseIndex;
							"save move track".postln;
							this.saveTmp;
						};
					};
				};
			}
		}
	}


	// mouse up

	bigramCanvasMouseUpAction {
		^{ |view, x, y, modifiers|

			var pulseIndex = (x / pulseWidth).floor.asInteger;
			var trackIndex = (y / trackHeight).floor.asInteger;

			var bpd = bigramEditor.getBPDfromPulseIndex(pulseIndex);
			var track = bigramEditor.bigramTracks.at(trackIndex);

			if (playButton.value == 0) {

				resizeRegion = nil;
				moveRegion = nil;

				switch (mode)
				{\pen} {

					if (track.getRegionAtBPD(bpd).isNil) {
						endRegionBPD = bigramEditor.getBPDfromPulseIndex(pulseIndex);

						if (startRegionBPD.isNil.not) {

							// check that region does not overlap internally
							var startIndex = bigramEditor.getPulseIndexFromBPD(startRegionBPD).asInteger.postln;
							var endIndex = pulseIndex.asInteger.postln;
							var hasRegion = List.new;
							"CHECK ROUTINE".postln;

							// reverse order if created backwards
							if (endIndex < startIndex) {
								var aux = startIndex;
								startIndex = endIndex;
								endIndex = aux;
								//
								aux = startRegionBPD;
								startRegionBPD = endRegionBPD;
								endRegionBPD = aux;
							};
							(startIndex..endIndex).select{|i|
								hasRegion.add(track.getRegionAtBPD(bigramEditor.getBPDfromPulseIndex(i)));
								// this comparison has no effect
								// but I don't know why I could not manage to get it working with .do
								1==1;
							};

							if (hasRegion.select(_.isNil.not).size == 0 ) {

								// create region
								track.addRegion(startRegionBPD,endRegionBPD);
							}
						}
					}
				};
				this.updateView;
			}
		}
	}

	// key down

	bigramViewKeyDownAction {
		^{ |view, char, modifiers, unicode, keycode, key|
			key.postln;
			modifiers.postln;

			switch (key)

			// space: play/pause
			{32} {
				// change value
				if (playButton.value == 0) {
					//play
					playButton.value_(1);
					bigramEditor.play(cursorPulseIndex,loopPulseIndex,loop);
				} {
					//stop
					playButton.value_(0);
					bigramEditor.pause;
				};
			}

			// control z -> undo
			{90} {
				if (modifiers == 262144) {
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
				};
				if (modifiers == 393216) { //redo
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
			}






		}
	}

	bigramCanvasKeyDownAction {
		^{ |view, char, modifiers, unicode, keycode, key|
			key.postln;

			switch (key)
			// control: hold selection
			{16777249} {holdSelection=true}

		}
	}

	bigramCanvasKeyUpAction {
		^{ |view, char, modifiers, unicode, keycode, key|

			switch (key)
			// control: hold selection
			{16777249} {holdSelection=false}

		}
	}

}
