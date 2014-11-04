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

BigramNote {

	var <region;
	var <editor;

	var <>pitch;
	var <>bpd;
	var <>isSelected;

	var <>duration; // <--- ??
	var <>midiAmp = 127; // <--- ??

	*new { |pitch, bpd, region, editor|
		^super.new.init(pitch,bpd, region, editor)
	}

	init { |myPitch, myBpd, myRegion, myEditor|

		pitch = myPitch ? BPitch.new;
		bpd = myBpd ? BPD.new;

		isSelected = false;

		// region = myRegion;
		// editor = region.bigramEditor;

	}

	getType {
		^pitch.getType;
	}

	copy {
		^BigramNote.new(pitch.copy,bpd.copy)
	}



	///////////////////// HACER A PARTIR DE AQUI ///////////////7
	//
	// absPos {
	// 	^beat + relativePos;
	// }
	//
	// *getAbsPitch { |height, pitchClass|
	// 	^(height*12) + pitchClass;
	// }
	//
	// *getAbsPos { |beat, relativePos|
	// 	^beat + relativePos;
	// }
	//
	// *getRelativePitch { |absPitch|
	// 	var height, pitchClass;
	// 	height = (absPitch / 12).floor;
	// 	pitchClass = (absPitch % 12);
	// 	^[height, pitchClass];
	// }
	//
	// *getRelativePos { |absPos|
	// 	var beat, relativePos;
	// 	beat = absPos.floor;
	// 	relativePos = absPos - beat;
	// 	^[beat, relativePos]
	// }
	//
	//

	movePitch { |semitones=1|
		if (semitones.sign > 0) {
			pitch.up(semitones)
		} {
			pitch.down(semitones.abs)
		}
	}


/*	moveUp { |semitones = 1|
		pitch.up(semitones)
	}

	moveDown { |semitones = 1|
		pitch.down(semitones)
	}*/

	// moveLeft { |amount=1|
	// 	var pos, newPos;
	// 	pos = beat + relativePos;
	// 	newPos = pos - amount;
	// 	if (newPos < 0) {newPos = 0};
	//
	// 	beat = newPos.floor;
	// 	relativePos = newPos - beat;
	// }
	//
	// moveRight { |amount=1|
	// 	var pos, newPos;
	// 	pos = beat + relativePos;
	// 	newPos = pos + amount;
	//
	// 	beat = newPos.floor;
	// 	relativePos = newPos - beat;
	//
	// }
	//
	// setClass { |newPitchClass|
	// 	pitchClass = newPitchClass;
	// 	type = this.getType;
	// }
	//
	// ////// note comparison
	//
	//
	atPosition { |aBpd|
		^bpd.equal(aBpd)
	}

	isAfter { |aBpd|
		^bpd.after(aBpd)
	}

	isAfterEqual { |aBpd|
		^bpd.afterEqual(aBpd)
	}

	isBefore { |aBpd|
		^bpd.before(aBpd)
	}

	isBeforeEqual { |aBpd|
		^bpd.beforeEqual(aBpd)
	}

	atPitch { |aPitch|
		^pitch.equal(aPitch);
	}

	isHigher { |aPitch|
		^pitch.higher(aPitch);
	}

	isHigherEqual { |aPitch|
		^pitch.higherEqual(aPitch);
	}

	isLower { |aPitch|
		^pitch.lower(aPitch);
	}

	isLowerEqual { |aPitch|
		^pitch.lowerEqual(aPitch);
	}

	sameAs { |aNote|
		^(bpd.equal(aNote.bpd) && pitch.equal(aNote.pitch))
	}


	////// other
	print {
		[[pitch.height,pitch.pClass],[bpd.bar,bpd.pulse,bpd.division]].postln;
	}
}
