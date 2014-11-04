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

PD { // pulse, division bigram measure system ---> how many pulses and divisions has a bar??


	var <pulse;
	var <division;

	*new { |p=4, d=2|
		^super.new.init(p,d);
	}

	init { |p=4, d=2|

		if (p<1) {pulse=1} {pulse=p};
		if (d<1) {division=1} {division=d};
	}

	getDrawableString {
		^(pulse.asString +/+ division.asString);
	}

}


BPD { // bar, pulse, division bigram measure system ---> absolute positioning

	var <note; // reference to the note
	var <editor;

	var <bar;
	var <pulse;
	var <division;

	*new { |b=0, p=0, d=0, note|
		^super.new.init(note).set(b,p,d);
	}
	init { |n|
		note = n;
		// editor = note.editor;
	}

	set { |b=0,p=0, d=0|

		if (b<0) {bar=0} {bar=b};
		if (p<0) {pulse=0} {pulse=p};
		if (d<0) {division=0} {division=d};
	}

	print {
		[bar,pulse,division].postln;
	}

	copy {
		^BPD.new(bar,pulse,division);
	}



	///////////////////////// TEMPORAL COMPARISON /////////////////////////

	// -1 if bpd1 < bpd2, 0 if bpd1 == bpd2, 1 if bpd1 > bpd2
	*relativePos { |bpd1,bpd2|
		var ans = switch ((bpd1.bar - bpd2.bar).sign.asInteger)
		{-1} {-1}
		{1} {1}
		{0} {switch ((bpd1.pulse - bpd2.pulse).sign.asInteger)
			{-1} {-1}
			{1} {1}
			{0} {(bpd1.division - bpd2.division).sign.asInteger}
		};
		^ans;
	}

	equal { |bpd|
		if (this.class.relativePos(this,bpd) == 0) {^true} {^false};
	}

	after { |bpd|
		if (this.class.relativePos(this,bpd) == 1) {^true} {^false};
	}

	before { |bpd|
		if (this.class.relativePos(this,bpd) == -1) {^true} {^false};
	}

	afterEqual { |bpd|
		if (this.class.relativePos(this,bpd) != -1) {^true} {^false};
	}

	beforeEqual { |bpd|
		if (this.class.relativePos(this,bpd) != 1) {^true} {^false};
	}




///////////////////////// TEMPORAL MANAGING /////////////////////////

/*	right { |divisions|
// we need to know the measures structure
// and also keep inside region borders
var measures = editor.measuresFromBars;
}*/

// use set

}


BPitch {

	var <note; // reference to the note

	var <height; // "octave"
	var <pClass; // note type (chromatic)
	var <type; // \b(lack) or \w(hite)

	*new { |h=4,c=9,n|
		^super.new.init(h,c,n)
	}

	init { |h=4,c=9,n| // A4
		height = h;
		pClass = c;
		type = this.getType;

		note = n;
	}

	getType {
		if (pClass.asInt.odd) {
			^\w;
		} {
			^\b;
		}
	}

	absPitch {
		^(height*12) + pClass;
	}

	copy {
		^BPitch.new(height,pClass);
	}

	///////////////////////// PITCH COMPARISON /////////////////////////

	// -1 if pitch1 < pitch2, 0 if pitch1 == pitch2, 1 if pitch1 > pitch2
	* relativePos { |pitch1,pitch2|
		^(pitch1.absPitch - pitch2.absPitch).sign;
	}

	equal { |aPitch|
		if (this.class.relativePos(this,aPitch) == 0) {^true} {^false}
	}

	higher { |aPitch|
		if (this.class.relativePos(this,aPitch) == 1) {^true} {^false}
	}

	lower { |aPitch|
		if (this.class.relativePos(this,aPitch) == -1) {^true} {^false}
	}

	higherEqual { |aPitch|
		if (this.class.relativePos(this,aPitch) != -1) {^true} {^false};
	}

	lowerEqual { |aPitch|
		if (this.class.relativePos(this,aPitch) != 1) {^true} {^false};
	}


	print {
		[height,pClass].postln;
	}

	///////////////////////// PITCH MANAGING /////////////////////////

	up { |semitones|
		var newPClass = pClass + semitones;
		if (newPClass > 11) {
			pClass = newPClass % 12;
			height = height + (newPClass /12).floor;
		} {
			pClass = newPClass;
		};
		type = this.getType;
	}

	down { |semitones|
		var newPClass = pClass - semitones;
		if (newPClass < 0) {
			pClass = newPClass % 12;
			height = height - 1 - (newPClass /12).neg.floor;
		} {
			pClass = newPClass;
		};
		type = this.getType;
	}


}
