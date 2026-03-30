// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.29;

import {ERC20} from "./ERC20.sol";
import {ERC20Burnable} from "./ERC20Burnable.sol";
import {Ownable} from "./Ownable.sol";




contract DecentralizedCoin is  ERC20, ERC20Burnable, Ownable {

    error DecentralizedCoin__AmountMustBeMoreThanZero();
       error DecentralizedCoin__BurnAmountExceedsBalance();
       error DecentralizedCoin__NotZeroAddress();


   constructor(string memory name_, string memory symbol_) ERC20(name_,symbol_) Ownable(msg.sender) { }

        function burn(address _from, uint256 _amount) public  onlyOwner returns (bool){
        uint256 balance = balanceOf(_from);
        if (_amount <= 0) {
        revert DecentralizedCoin__AmountMustBeMoreThanZero();
        }
        if (balance < _amount) {
        revert DecentralizedCoin__BurnAmountExceedsBalance();
        }

        _burn(_from,_amount);
            return true;
        }

        function mint(address _to, uint256 _amount) public  onlyOwner returns (bool) {
        if (_to == address(0)) {
        revert DecentralizedCoin__NotZeroAddress();
        }
        if (_amount <= 0) {
        revert DecentralizedCoin__AmountMustBeMoreThanZero();
        }
        _mint(_to, _amount);
            return true;
        }

        }




